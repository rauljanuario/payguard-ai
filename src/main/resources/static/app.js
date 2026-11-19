const auditForm = document.querySelector("#auditForm");
const auditButton = document.querySelector("#auditButton");
const statusBadge = document.querySelector("#statusBadge");
const resultPanel = document.querySelector("#resultPanel");
const severityBadge = document.querySelector("#severityBadge");
const breakingChangeResult = document.querySelector("#breakingChangeResult");
const discrepancies = document.querySelector("#discrepancies");
const suggestedDocumentation = document.querySelector("#suggestedDocumentation");
const approveButton = document.querySelector("#approveButton");
const rejectButton = document.querySelector("#rejectButton");
const copyResultButton = document.querySelector("#copyResultButton");
const refreshButton = document.querySelector("#refreshButton");
const history = document.querySelector("#history");
const API_BASE_URL = "http://127.0.0.1:8080";

let currentAuditId = null;

auditForm.addEventListener("submit", createAudit);
approveButton.addEventListener("click", () => reviewAudit("APPROVED"));
rejectButton.addEventListener("click", () => reviewAudit("REJECTED"));
copyResultButton.addEventListener("click", () =>
    copyMarkdown(suggestedDocumentation.textContent, copyResultButton));
refreshButton.addEventListener("click", loadHistory);

async function createAudit(event) {
    event.preventDefault();
    setLoading(true);

    const request = {
        moduleName: document.querySelector("#moduleName").value.trim(),
        sourceCode: document.querySelector("#sourceCode").value,
        currentDocumentation:
            document.querySelector("#currentDocumentation").value
    };

    try {
        const response = await fetch(`${API_BASE_URL}/api/audits`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        const body = await readJsonResponse(response);
        if (!response.ok) {
            throw new Error(body.message || "Falha na auditoria");
        }

        renderResult(body);
        await loadHistory();
        statusBadge.textContent = "Auditoria concluída";
    } catch (error) {
        statusBadge.textContent = "Erro";
        alert(error.message);
    } finally {
        setLoading(false);
    }
}

function renderResult(audit) {
    currentAuditId = audit.id;
    resultPanel.hidden = false;
    severityBadge.textContent = audit.severity;
    severityBadge.className =
        `severity-badge severity-${audit.severity}`;
    breakingChangeResult.textContent = audit.hasBreakingChange
        ? "Breaking change detectada"
        : "Nenhuma breaking change detectada";

    discrepancies.replaceChildren();
    if (!audit.discrepancies || audit.discrepancies.length === 0) {
        discrepancies.append(createListItem("Nenhuma divergência encontrada."));
    } else {
        audit.discrepancies.forEach(discrepancy =>
            discrepancies.append(createListItem(discrepancy)));
    }

    suggestedDocumentation.textContent = audit.suggestedDocMarkdown || "";
    updateResultReviewActions(audit.reviewStatus);
}

function createListItem(text) {
    const item = document.createElement("li");
    item.textContent = text;
    return item;
}

async function reviewAudit(reviewStatus, auditId = currentAuditId, actionContainer = null) {
    if (auditId === null) {
        return;
    }

    const buttons = actionContainer
        ? [...actionContainer.querySelectorAll("button")]
        : [approveButton, rejectButton];
    buttons.forEach(button => {
        button.disabled = true;
    });

    try {
        const response = await fetch(
            `${API_BASE_URL}/api/audits/${auditId}/review`,
            {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ reviewStatus })
            });
        const body = await readJsonResponse(response);
        if (!response.ok) {
            throw new Error(body.message || "Falha ao salvar a revisão");
        }

        if (auditId === currentAuditId) {
            statusBadge.textContent =
                reviewStatus === "APPROVED"
                    ? "Sugestão aprovada"
                    : "Sugestão descartada";
            updateResultReviewActions(reviewStatus);
        }
        await loadHistory();
    } catch (error) {
        alert(error.message);
    } finally {
        buttons.forEach(button => {
            button.disabled = false;
        });
    }
}

async function loadHistory() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/audits`);
        const audits = await readJsonResponse(response);
        if (!response.ok) {
            throw new Error(audits.message || "Falha ao carregar o histórico");
        }

        renderHistory(audits);
    } catch (error) {
        history.replaceChildren(
            createHistoryMessage(error.message));
    }
}

async function readJsonResponse(response) {
    const contentType = (response.headers.get("content-type") || "").toLowerCase();
    const responseText = await response.text();

    if (!contentType.includes("application/json")) {
        throw new Error(
            `O servidor respondeu com conteúdo inválido (HTTP ${response.status}).`
        );
    }

    try {
        return JSON.parse(responseText);
    } catch {
        throw new Error("O servidor retornou uma resposta JSON inválida.");
    }
}

function renderHistory(audits) {
    history.replaceChildren();

    if (audits.length === 0) {
        history.append(createHistoryMessage("Nenhuma auditoria registrada."));
        return;
    }

    audits.forEach(audit => {
        const item = document.createElement("details");
        item.className = "history-item";

        const summary = document.createElement("summary");
        const header = document.createElement("div");
        header.className = "history-item-header";

        const moduleName = document.createElement("span");
        moduleName.className = "module-name";
        moduleName.textContent = audit.moduleName;

        const reviewStatus = audit.reviewStatus || "PENDENTE";
        const badge = document.createElement("span");
        badge.className = `review-badge review-${reviewStatus.toLowerCase()}`;
        badge.textContent = formatReviewStatus(reviewStatus);

        header.append(moduleName, badge);

        const details = document.createElement("p");
        details.textContent =
            `${audit.severity} · ${audit.hasBreakingChange
                ? "Breaking change"
                : "Sem breaking change"} · ${formatDate(audit.createdAt)}`;

        summary.append(header, details);

        const content = document.createElement("div");
        content.className = "history-item-details";

        const discrepanciesHeading = document.createElement("h3");
        discrepanciesHeading.textContent = "Divergências";
        const discrepanciesList = document.createElement("ul");
        const auditDiscrepancies = audit.discrepancies || [];
        if (auditDiscrepancies.length === 0) {
            discrepanciesList.append(
                createListItem("Nenhuma divergência encontrada."));
        } else {
            auditDiscrepancies.forEach(discrepancy =>
                discrepanciesList.append(createListItem(discrepancy)));
        }

        const documentationHeading = document.createElement("h3");
        documentationHeading.textContent = "Documentação sugerida";
        const documentation = document.createElement("pre");
        const documentationCode = document.createElement("code");
        documentationCode.textContent = audit.suggestedDocMarkdown || "";
        documentation.append(documentationCode);
        const copyButton = document.createElement("button");
        copyButton.className = "secondary copy-button";
        copyButton.type = "button";
        copyButton.textContent = "Copiar Markdown";
        copyButton.addEventListener("click", () =>
            copyMarkdown(documentationCode.textContent, copyButton));

        content.append(
            discrepanciesHeading,
            discrepanciesList,
            documentationHeading,
            documentation,
            copyButton);

        const actions = document.createElement("div");
        actions.className = "review-actions";
        if (reviewStatus === "PENDENTE") {
            actions.append(
                createHistoryReviewButton(
                    "Aprovar", "APPROVED", audit.id, actions),
                createHistoryReviewButton(
                    "Descartar", "REJECTED", audit.id, actions));
        }
        content.append(actions);
        item.append(summary, content);
        history.append(item);
    });
}

function createHistoryReviewButton(label, reviewStatus, auditId, actionContainer) {
    const button = document.createElement("button");
    button.className = reviewStatus === "APPROVED" ? "approve" : "reject";
    button.type = "button";
    button.textContent = label;
    button.addEventListener("click", () =>
        reviewAudit(reviewStatus, auditId, actionContainer));
    return button;
}

function formatReviewStatus(reviewStatus) {
    return {
        PENDENTE: "PENDENTE",
        APPROVED: "APROVADO",
        REJECTED: "DESCARTADO"
    }[reviewStatus] || reviewStatus;
}

function updateResultReviewActions(reviewStatus) {
    const isPending = !reviewStatus;
    approveButton.hidden = !isPending;
    rejectButton.hidden = !isPending;
}

async function copyMarkdown(markdown, button) {
    try {
        await navigator.clipboard.writeText(markdown);
        const originalLabel = button.textContent;
        button.textContent = "Markdown copiado";
        setTimeout(() => {
            button.textContent = originalLabel;
        }, 1500);
    } catch {
        alert("Não foi possível copiar o Markdown.");
    }
}

function createHistoryMessage(message) {
    const element = document.createElement("p");
    element.className = "history-empty";
    element.textContent = message;
    return element;
}

function formatDate(value) {
    if (!value) {
        return "";
    }
    return new Date(value).toLocaleString("pt-BR");
}

function setLoading(isLoading) {
    auditButton.disabled = isLoading;
    auditButton.textContent = isLoading
        ? "Auditando..."
        : "Executar auditoria";
}

loadHistory();

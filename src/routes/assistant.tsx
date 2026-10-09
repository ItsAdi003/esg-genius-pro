import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Bot, FileText, Info, Send, User } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { AppLayout } from "@/components/app-layout";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { askDocumentAssistant, AssistantApiError, type AssistantCitation } from "@/lib/assistant-api";
import { companyEsgQueryKeys, getCompanies } from "@/lib/company-esg-api";
import { formatEvidenceSourceLabel } from "@/lib/compliance-api";
import {
  documentQueryKeys,
  formatReportingYear,
  listDocuments,
  type DocumentSummary,
} from "@/lib/document-api";
import { suggestedQuestions } from "@/lib/esg-data";

export const Route = createFileRoute("/assistant")({
  head: () => ({
    meta: [
      { title: "AI ESG Assistant | ESGenius" },
      {
        name: "description",
        content:
          "Ask questions about one uploaded document. Answers cite passages from that document when evidence is found.",
      },
      { property: "og:title", content: "AI ESG Assistant | ESGenius" },
      {
        property: "og:description",
        content: "Chat about a selected uploaded document, with citations when evidence is found.",
      },
    ],
  }),
  component: Assistant,
});

const MAX_QUESTION_LENGTH = 500;

type ChatMessage = {
  role: "user" | "assistant";
  content: string;
  grounded?: boolean;
  citations?: AssistantCitation[];
  documentName?: string;
  error?: boolean;
};

const initial: ChatMessage[] = [
  {
    role: "assistant",
    content:
      "Select a ready document, then ask a question. Answers use that document's extracted text and cite a page or source chunk when evidence is found.",
  },
];

function citationSource(citation: AssistantCitation) {
  return formatEvidenceSourceLabel({
    chunkIndex: citation.chunkIndex,
    pageNumber: citation.pageNumber,
    text: citation.snippet,
    retrievalScore: 0,
  });
}

function Citations({ message }: { message: ChatMessage }) {
  if (!message.citations?.length) return null;
  return (
    <div className="mt-3 space-y-2">
      {message.citations.map((citation, i) => (
        <div key={i} className="rounded-lg border-l-4 border-primary bg-accent/50 p-3">
          <div className="flex flex-wrap items-center gap-2 text-[11px]">
            <span className="flex items-center gap-1 rounded border border-border bg-card px-2 py-0.5 text-muted-foreground">
              <FileText className="size-3" />
              {message.documentName ? `${message.documentName} · ` : ""}
              {citationSource(citation)}
            </span>
          </div>
          <p className="mt-2 text-xs italic leading-relaxed">“{citation.snippet}”</p>
        </div>
      ))}
    </div>
  );
}

function Assistant() {
  const [messages, setMessages] = useState<ChatMessage[]>(initial);
  const [input, setInput] = useState("");
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<number | null>(null);
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  const companiesQuery = useQuery({
    queryKey: companyEsgQueryKeys.companies(),
    queryFn: getCompanies,
  });
  const companies = useMemo(
    () => companiesQuery.data ?? [],
    [companiesQuery.data],
  );

  useEffect(() => {
    if (selectedOrganizationId != null) return;
    const firstCompany = companies[0];
    if (firstCompany === undefined) return;
    setSelectedOrganizationId(firstCompany.id);
  }, [companies, selectedOrganizationId]);

  const documentsQuery = useQuery({
    queryKey:
      selectedOrganizationId != null
        ? documentQueryKeys.list(selectedOrganizationId)
        : documentQueryKeys.all,
    queryFn: () => listDocuments(selectedOrganizationId!),
    enabled: selectedOrganizationId != null,
  });

  const readyDocuments = (documentsQuery.data ?? []).filter(
    (document) => document.status === "READY",
  );
  const selectedDocument = readyDocuments.find((document) => document.id === selectedDocumentId);

  const askMutation = useMutation({
    mutationFn: ({ documentId, question }: { documentId: number; question: string }) =>
      askDocumentAssistant(documentId, question),
  });

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages, askMutation.isPending]);

  const send = async (text: string) => {
    const question = text.trim();
    if (!question || selectedDocumentId == null || askMutation.isPending) return;
    if (question.length > MAX_QUESTION_LENGTH) {
      setMessages((current) => [
        ...current,
        { role: "user", content: question },
        {
          role: "assistant",
          content: `Question must be at most ${MAX_QUESTION_LENGTH} characters.`,
          error: true,
        },
      ]);
      setInput("");
      return;
    }

    const documentName = selectedDocument?.originalFilename;
    setMessages((current) => [...current, { role: "user", content: question }]);
    setInput("");

    try {
      const result = await askMutation.mutateAsync({
        documentId: selectedDocumentId,
        question,
      });
      setMessages((current) => [
        ...current,
        {
          role: "assistant",
          content: result.answer,
          grounded: result.grounded,
          ...(result.grounded ? { citations: result.citations } : {}),
          ...(documentName !== undefined ? { documentName } : {}),
        },
      ]);
    } catch (error) {
      const message =
        error instanceof AssistantApiError
          ? error.message
          : "The assistant could not answer this question.";
      setMessages((current) => [
        ...current,
        { role: "assistant", content: message, error: true },
      ]);
    }
  };

  return (
    <AppLayout
      title="ESG Compliance Assistant"
      description="Ask a question about one uploaded document"
    >
      <div className="mb-4 rounded-lg border border-border/80 bg-muted/30 px-4 py-3">
        <div className="flex gap-2.5">
          <Info className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
          <p className="text-xs leading-relaxed text-muted-foreground">
            AI-assisted — verify before relying on this for compliance decisions. Answers are
            grounded in the selected document only and do not replace compliance review.
          </p>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="space-y-1.5">
          <p className="text-sm font-medium">Company</p>
          {companiesQuery.isLoading ? (
            <Skeleton className="h-10 w-56" />
          ) : (
            <Select
              {...(selectedOrganizationId != null
                ? { value: String(selectedOrganizationId) }
                : {})}
              onValueChange={(value) => {
                setSelectedOrganizationId(Number(value));
                setSelectedDocumentId(null);
              }}
            >
              <SelectTrigger className="w-56">
                <SelectValue placeholder="Select company" />
              </SelectTrigger>
              <SelectContent>
                {companies.map((company) => (
                  <SelectItem key={company.id} value={String(company.id)}>
                    {company.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
        <div className="space-y-1.5">
          <p className="text-sm font-medium">Document</p>
          {documentsQuery.isLoading ? (
            <Skeleton className="h-10 w-72" />
          ) : (
            <Select
              {...(selectedDocumentId != null ? { value: String(selectedDocumentId) } : {})}
              onValueChange={(value) => setSelectedDocumentId(Number(value))}
              disabled={readyDocuments.length === 0}
            >
              <SelectTrigger className="w-72">
                <SelectValue
                  placeholder={
                    readyDocuments.length === 0 ? "No ready documents" : "Select a document"
                  }
                />
              </SelectTrigger>
              <SelectContent>
                {readyDocuments.map((document) => (
                  <SelectItem key={document.id} value={String(document.id)}>
                    {documentLabel(document)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
      </div>

      {companiesQuery.isError && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Unable to load companies</AlertTitle>
          <AlertDescription>{companiesQuery.error.message}</AlertDescription>
        </Alert>
      )}
      {documentsQuery.isError && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Unable to load documents</AlertTitle>
          <AlertDescription>{documentsQuery.error.message}</AlertDescription>
        </Alert>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_300px]">
        <section className="surface-card flex h-[68vh] min-h-[520px] flex-col">
          <header className="flex items-center gap-3 border-b border-border px-5 py-3">
            <span className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Bot className="size-4" />
            </span>
            <div>
              <p className="text-sm font-semibold">ESG Compliance Assistant</p>
              <p className="text-[11px] text-muted-foreground">
                {selectedDocument
                  ? `Using ${selectedDocument.originalFilename}`
                  : "Select a ready document to ask a question"}
              </p>
            </div>
          </header>

          <div className="flex-1 space-y-5 overflow-y-auto px-5 py-5">
            {messages.map((message, index) => (
              <div
                key={index}
                className={message.role === "user" ? "flex justify-end" : "flex gap-3"}
              >
                {message.role === "assistant" && (
                  <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                    <Bot className="size-4" />
                  </span>
                )}
                <div
                  className={
                    message.role === "user"
                      ? "max-w-[80%] rounded-xl rounded-tr-sm bg-primary px-4 py-2.5 text-sm text-primary-foreground"
                      : message.grounded === false
                        ? "max-w-[85%] rounded-xl rounded-tl-sm border border-warning/40 bg-warning-soft/40 px-4 py-3"
                        : message.error
                          ? "max-w-[85%] rounded-xl rounded-tl-sm border border-danger/30 bg-danger-soft/30 px-4 py-3"
                          : "max-w-[85%] rounded-xl rounded-tl-sm border border-border bg-muted/40 px-4 py-3"
                  }
                >
                  {message.grounded === false && (
                    <p className="mb-1 text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                      Not found in this document
                    </p>
                  )}
                  <p className="whitespace-pre-line text-sm leading-relaxed">{message.content}</p>
                  <Citations message={message} />
                </div>
                {message.role === "user" && (
                  <span className="ml-3 mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-secondary text-secondary-foreground">
                    <User className="size-4" />
                  </span>
                )}
              </div>
            ))}
            {askMutation.isPending && (
              <div className="flex gap-3">
                <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                  <Bot className="size-4" />
                </span>
                <div className="rounded-xl rounded-tl-sm border border-border bg-muted/40 px-4 py-3 text-sm text-muted-foreground">
                  Searching the selected document…
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          <footer className="border-t border-border px-5 py-4">
            <form
              className="flex gap-2"
              onSubmit={(event) => {
                event.preventDefault();
                void send(input);
              }}
            >
              <Input
                value={input}
                maxLength={MAX_QUESTION_LENGTH}
                onChange={(event) => setInput(event.target.value)}
                placeholder={
                  selectedDocumentId == null
                    ? "Select a document before asking…"
                    : "Ask about this document…"
                }
                disabled={selectedDocumentId == null || askMutation.isPending}
              />
              <Button type="submit" disabled={selectedDocumentId == null || askMutation.isPending}>
                <Send className="size-4" />
              </Button>
            </form>
            <p className="mt-2 flex items-center gap-1.5 text-[11px] text-muted-foreground">
              <Info className="size-3" />
              AI-assisted — verify before relying on this for compliance decisions.
            </p>
          </footer>
        </section>

        <aside className="surface-card h-fit p-5">
          <h2 className="text-sm font-semibold">Suggested questions</h2>
          <div className="mt-3 flex flex-col gap-2">
            {suggestedQuestions.map((question) => (
              <button
                key={question}
                type="button"
                disabled={selectedDocumentId == null || askMutation.isPending}
                onClick={() => void send(question)}
                className="rounded-lg border border-border bg-muted/40 px-3 py-2 text-left text-xs leading-relaxed transition-colors hover:border-primary/40 hover:bg-accent/60 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {question}
              </button>
            ))}
          </div>
          <div className="mt-5 rounded-lg border border-border bg-card p-3">
            <p className="text-xs font-medium">This document only</p>
            <p className="mt-1 text-[11px] leading-relaxed text-muted-foreground">
              Each answer uses the selected document. It does not search other uploads and does
              not replace a compliance assessment.
            </p>
          </div>
        </aside>
      </div>
    </AppLayout>
  );
}

function documentLabel(document: DocumentSummary): string {
  const year = formatReportingYear(document.reportingYear);
  return year === "—" ? document.originalFilename : `${document.originalFilename} · ${year}`;
}

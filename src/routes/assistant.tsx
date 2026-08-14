import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { Bot, Send, User, FileText, Info } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  cannedAnswers,
  defaultAnswer,
  suggestedQuestions,
  type ChatMessage,
} from "@/lib/esg-data";

export const Route = createFileRoute("/assistant")({
  head: () => ({
    meta: [
      { title: "AI ESG Assistant | ESGenius" },
      {
        name: "description",
        content:
          "Ask evidence-backed questions about ESG compliance gaps, with citations to source documents and page numbers.",
      },
      { property: "og:title", content: "AI ESG Assistant | ESGenius" },
      {
        property: "og:description",
        content: "Chat-based ESG compliance assistance grounded in your organization's documents.",
      },
    ],
  }),
  component: Assistant,
});

const initial: ChatMessage[] = [
  {
    role: "assistant",
    content:
      "Hello Priya. I can answer questions about ABC Industries' ESG reporting readiness against SEBI BRSR. Overall readiness is currently 78%, based on 12 analysed documents. Ask about a requirement, an ESG category, or your open gaps.",
  },
];

function Citations({ message }: { message: ChatMessage }) {
  if (!message.citations?.length) return null;
  return (
    <div className="mt-3 space-y-2">
      {message.citations.map((c, i) => (
        <div key={i} className="rounded-lg border-l-4 border-primary bg-accent/50 p-3">
          <div className="flex flex-wrap items-center gap-2 text-[11px]">
            <span className="rounded border border-border bg-card px-2 py-0.5 font-medium">
              {c.requirement}
            </span>
            <span className="flex items-center gap-1 rounded border border-border bg-card px-2 py-0.5 text-muted-foreground">
              <FileText className="size-3" />
              {c.document}
              {c.page > 0 ? ` · Page ${c.page}` : ""}
            </span>
          </div>
          <p className="mt-2 text-xs italic leading-relaxed">“{c.snippet}”</p>
        </div>
      ))}
    </div>
  );
}

function Assistant() {
  const [messages, setMessages] = useState<ChatMessage[]>(initial);
  const [input, setInput] = useState("");

  const send = (text: string) => {
    const q = text.trim();
    if (!q) return;
    const answer = cannedAnswers[q] ?? defaultAnswer;
    setMessages((m) => [...m, { role: "user", content: q }, answer]);
    setInput("");
  };

  return (
    <AppLayout
      title="ESG Compliance Assistant"
      description="Evidence-linked answers generated from your documents and ESG framework sources"
    >
      <div className="grid gap-4 lg:grid-cols-[1fr_300px]">
        <section className="surface-card flex h-[68vh] min-h-[520px] flex-col">
          <header className="flex items-center gap-3 border-b border-border px-5 py-3">
            <span className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Bot className="size-4" />
            </span>
            <div>
              <p className="text-sm font-semibold">ESG Compliance Assistant</p>
              <p className="text-[11px] text-muted-foreground">
                AI-assisted · SEBI BRSR · FY 2025-26
              </p>
            </div>
          </header>

          <div className="flex-1 space-y-5 overflow-y-auto px-5 py-5">
            {messages.map((m, i) => (
              <div key={i} className={m.role === "user" ? "flex justify-end" : "flex gap-3"}>
                {m.role === "assistant" && (
                  <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-accent text-accent-foreground">
                    <Bot className="size-4" />
                  </span>
                )}
                <div
                  className={
                    m.role === "user"
                      ? "max-w-[80%] rounded-xl rounded-tr-sm bg-primary px-4 py-2.5 text-sm text-primary-foreground"
                      : "max-w-[85%] rounded-xl rounded-tl-sm border border-border bg-muted/40 px-4 py-3"
                  }
                >
                  <p className="whitespace-pre-line text-sm leading-relaxed">{m.content}</p>
                  <Citations message={m} />
                </div>
                {m.role === "user" && (
                  <span className="ml-3 mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-secondary text-secondary-foreground">
                    <User className="size-4" />
                  </span>
                )}
              </div>
            ))}
          </div>

          <footer className="border-t border-border px-5 py-4">
            <form
              className="flex gap-2"
              onSubmit={(e) => {
                e.preventDefault();
                send(input);
              }}
            >
              <Input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ask about a requirement, category or evidence gap…"
              />
              <Button type="submit">
                <Send className="size-4" />
              </Button>
            </form>
            <p className="mt-2 flex items-center gap-1.5 text-[11px] text-muted-foreground">
              <Info className="size-3" />
              Responses are generated using organization documents and ESG framework sources, and
              support — not replace — human compliance review.
            </p>
          </footer>
        </section>

        <aside className="surface-card h-fit p-5">
          <h2 className="text-sm font-semibold">Suggested questions</h2>
          <div className="mt-3 flex flex-col gap-2">
            {suggestedQuestions.map((q) => (
              <button
                key={q}
                onClick={() => send(q)}
                className="rounded-lg border border-border bg-muted/40 px-3 py-2 text-left text-xs leading-relaxed transition-colors hover:border-primary/40 hover:bg-accent/60"
              >
                {q}
              </button>
            ))}
          </div>
          <div className="mt-5 rounded-lg border border-border bg-card p-3">
            <p className="text-xs font-medium">Grounding sources</p>
            <p className="mt-1 text-[11px] leading-relaxed text-muted-foreground">
              12 analysed documents · SEBI BRSR requirement library (56 disclosures)
            </p>
          </div>
        </aside>
      </div>
    </AppLayout>
  );
}

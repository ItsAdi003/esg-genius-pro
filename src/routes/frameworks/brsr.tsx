import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { AppLayout } from "@/components/app-layout";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { requirements } from "@/lib/esg-data";

export const Route = createFileRoute("/frameworks/brsr")({
  head: () => ({
    meta: [
      { title: "SEBI BRSR Requirements | ESGenius" },
      {
        name: "description",
        content:
          "Structured SEBI BRSR requirement library with ESG category, description, mandatory status and framework version.",
      },
      { property: "og:title", content: "SEBI BRSR Requirements | ESGenius" },
      {
        property: "og:description",
        content: "The BRSR requirement knowledge base used for ESG gap analysis.",
      },
    ],
  }),
  component: BrsrRequirements,
});

function BrsrRequirements() {
  const [tab, setTab] = useState("all");
  const rows = requirements.filter((r) => tab === "all" || r.category === tab);

  return (
    <AppLayout
      title="SEBI BRSR — Framework Requirements"
      description="Requirements stored in the ESG knowledge base · BRSR 2023 (v1.2)"
      actions={
        <Button variant="outline" asChild>
          <Link to="/frameworks">
            <ArrowLeft className="size-4" /> All frameworks
          </Link>
        </Button>
      }
    >
      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="Environmental">Environmental</TabsTrigger>
          <TabsTrigger value="Social">Social</TabsTrigger>
          <TabsTrigger value="Governance">Governance</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="surface-card mt-4 overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/50">
                <TableHead className="w-24">Req. ID</TableHead>
                <TableHead className="min-w-[220px]">Requirement Name</TableHead>
                <TableHead>ESG Category</TableHead>
                <TableHead className="min-w-[320px]">Description</TableHead>
                <TableHead>Mandatory / Optional</TableHead>
                <TableHead>Version</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="font-mono text-xs text-muted-foreground">{r.id}</TableCell>
                  <TableCell className="font-medium">{r.title}</TableCell>
                  <TableCell className="text-muted-foreground">{r.category}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{r.description}</TableCell>
                  <TableCell>
                    <span
                      className={
                        r.mandatory
                          ? "rounded-full border border-primary/25 bg-accent px-2.5 py-0.5 text-xs font-medium text-accent-foreground"
                          : "rounded-full border border-border bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground"
                      }
                    >
                      {r.mandatory ? "Mandatory" : "Optional"}
                    </span>
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">{r.version}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>
      <p className="mt-3 text-xs text-muted-foreground">
        Showing {rows.length} indexed requirements of 56 total BRSR disclosures in the knowledge
        base.
      </p>
    </AppLayout>
  );
}

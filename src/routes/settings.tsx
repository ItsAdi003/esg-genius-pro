import { createFileRoute } from "@tanstack/react-router";
import { Building2, Library, Bot, Archive, Bell } from "lucide-react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ORG } from "@/lib/esg-data";

export const Route = createFileRoute("/settings")({
  head: () => ({
    meta: [
      { title: "Settings | ESGenius" },
      {
        name: "description",
        content:
          "Configure organization profile, ESG framework preferences, AI analysis thresholds, retention and notifications.",
      },
      { property: "og:title", content: "Settings | ESGenius" },
      {
        property: "og:description",
        content: "Workspace configuration for ESG compliance analysis.",
      },
    ],
  }),
  component: SettingsPage,
});

function Section({
  title,
  description,
  icon: Icon,
  children,
}: {
  title: string;
  description: string;
  icon: React.ElementType;
  children: React.ReactNode;
}) {
  return (
    <section className="surface-card p-5">
      <header className="flex items-start gap-3 border-b border-border pb-4">
        <span className="flex size-9 items-center justify-center rounded-lg bg-accent text-accent-foreground">
          <Icon className="size-4" />
        </span>
        <div>
          <h2 className="text-sm font-semibold">{title}</h2>
          <p className="text-xs text-muted-foreground">{description}</p>
        </div>
      </header>
      <div className="pt-4">{children}</div>
    </section>
  );
}

function ToggleRow({
  label,
  hint,
  defaultChecked,
}: {
  label: string;
  hint: string;
  defaultChecked?: boolean | undefined;
}) {
  return (
    <div className="flex items-start justify-between gap-4 py-2.5">
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{hint}</p>
      </div>
      <Switch defaultChecked={defaultChecked ?? false} />
    </div>
  );
}

function SettingsPage() {
  return (
    <AppLayout
      title="Settings"
      description="Workspace configuration for ESG compliance analysis"
      actions={<Button onClick={() => toast.success("Settings saved")}>Save changes</Button>}
    >
      <div className="grid gap-4 lg:grid-cols-2">
        <Section
          title="Organization Profile"
          description="Details used across analyses and reports"
          icon={Building2}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="org">Organization name</Label>
              <Input id="org" defaultValue={ORG.name} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="cin">Corporate identity number</Label>
              <Input id="cin" defaultValue="L12345MH2004PLC145678" />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="sector">Sector</Label>
              <Input id="sector" defaultValue="Industrial Manufacturing" />
            </div>
            <div className="space-y-1.5">
              <Label>Reporting period</Label>
              <Select defaultValue="2025-26">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="2025-26">FY 2025-26</SelectItem>
                  <SelectItem value="2024-25">FY 2024-25</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="contact">ESG contact</Label>
              <Input id="contact" defaultValue="priya.nair@abcindustries.in" />
            </div>
          </div>
        </Section>

        <Section
          title="ESG Framework Preferences"
          description="Frameworks applied during gap analysis"
          icon={Library}
        >
          <div className="space-y-1 divide-y divide-border">
            <ToggleRow label="SEBI BRSR" hint="Active framework · India · 56 requirements" defaultChecked />
            <ToggleRow label="GRI Standards" hint="Planned · Global" />
            <ToggleRow label="IFRS S1 / S2" hint="Planned · Global" />
            <ToggleRow label="ESRS" hint="Planned · European Union" />
          </div>
        </Section>

        <Section
          title="AI Analysis Settings"
          description="How evidence retrieval and assessment behave"
          icon={Bot}
        >
          <div className="space-y-5">
            <div>
              <div className="flex items-center justify-between">
                <Label>Minimum confidence for auto-assessment</Label>
                <span className="text-sm font-medium tabular-nums">80%</span>
              </div>
              <Slider defaultValue={[80]} max={100} step={5} className="mt-3" />
              <p className="mt-2 text-xs text-muted-foreground">
                Requirements below this threshold are routed to human review.
              </p>
            </div>
            <div className="space-y-1 divide-y divide-border">
              <ToggleRow
                label="Always cite source document and page"
                hint="Evidence snippets are attached to every assessment"
                defaultChecked
              />
              <ToggleRow
                label="Flag ambiguous evidence for human review"
                hint="Commitment-only language is never treated as quantitative evidence"
                defaultChecked
              />
              <ToggleRow
                label="Include optional disclosures in scoring"
                hint="Optional BRSR items contribute to the readiness score"
              />
            </div>
          </div>
        </Section>

        <Section
          title="Document Retention"
          description="How long uploaded evidence is kept"
          icon={Archive}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label>Retention period</Label>
              <Select defaultValue="7y">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1y">1 year</SelectItem>
                  <SelectItem value="3y">3 years</SelectItem>
                  <SelectItem value="7y">7 years</SelectItem>
                  <SelectItem value="forever">Indefinite</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Archive after</Label>
              <Select defaultValue="24m">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="12m">12 months</SelectItem>
                  <SelectItem value="24m">24 months</SelectItem>
                  <SelectItem value="36m">36 months</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="sm:col-span-2">
              <ToggleRow
                label="Retain extracted text after document deletion"
                hint="Keeps citations resolvable in historical reports"
              />
            </div>
          </div>
        </Section>

        <Section
          title="Notifications"
          description="When the workspace should alert your team"
          icon={Bell}
        >
          <div className="space-y-1 divide-y divide-border">
            <ToggleRow
              label="Document analysis completed"
              hint="Email when a document finishes processing"
              defaultChecked
            />
            <ToggleRow
              label="New high-priority gap detected"
              hint="Alert when a mandatory disclosure loses evidence"
              defaultChecked
            />
            <ToggleRow
              label="Weekly readiness digest"
              hint="Summary of score movement and open gaps"
              defaultChecked
            />
            <ToggleRow
              label="Human review reminders"
              hint="Nudge reviewers on items pending more than 7 days"
            />
          </div>
        </Section>
      </div>
    </AppLayout>
  );
}

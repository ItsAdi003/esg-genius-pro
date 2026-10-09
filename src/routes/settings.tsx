import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Building2, Library, Bot, Archive, Bell, UserCircle, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { AppLayout } from "@/components/app-layout";
import { PrototypeNotice } from "@/components/prototype-notice";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
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
import {
  fetchMe,
  formatResetIn,
  ME_LIMIT_ROWS,
  meQueryKeys,
  usageFraction,
  type UsageLimit,
} from "@/lib/me-api";
import { useSessionUser } from "@/lib/use-session-user";

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

function UsageLimitRow({ label, limit }: { label: string; limit: UsageLimit | null }) {
  if (limit === null) {
    return (
      <div className="flex items-center justify-between gap-4 py-2.5">
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">No limit</p>
      </div>
    );
  }

  const fraction = usageFraction(limit.used, limit.limit);
  const percent = Math.round(fraction * 100);

  return (
    <div className="space-y-2 py-2.5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs tabular-nums text-muted-foreground">
          {limit.used} of {limit.limit}
          {limit.used > 0 ? (
            <span className="ml-2">· resets {formatResetIn(limit.resetsInSeconds)}</span>
          ) : null}
        </p>
      </div>
      <div
        role="progressbar"
        aria-valuenow={limit.used}
        aria-valuemin={0}
        aria-valuemax={limit.limit}
        aria-label={`${label}: ${limit.used} of ${limit.limit}`}
        className="h-2 overflow-hidden rounded-full bg-muted"
      >
        <div
          className="h-full rounded-full bg-primary transition-[width]"
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}

function AccountUsageCard() {
  const { data, isLoading, isError, isSuccess, refetch, isFetching } = useQuery({
    queryKey: meQueryKeys.profile(),
    queryFn: fetchMe,
  });

  if (isSuccess && data === null) {
    return null;
  }

  return (
    <section className="surface-card mb-4 p-5">
      <header className="flex items-start gap-3 border-b border-border pb-4">
        <span className="flex size-9 items-center justify-center rounded-lg bg-accent text-accent-foreground">
          <UserCircle className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
          <h2 className="text-sm font-semibold">Account &amp; usage</h2>
          <p className="text-xs text-muted-foreground">
            Signed-in identity and rolling usage limits for this workspace.
          </p>
        </div>
      </header>

      <div className="pt-4">
        {isLoading ? (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Skeleton className="h-4 w-48" />
              <Skeleton className="h-5 w-16 rounded-full" />
            </div>
            <Skeleton className="h-3 w-full max-w-md" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : isError ? (
          <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
            <span>Could not load account usage.</span>
            <Button
              variant="outline"
              size="sm"
              disabled={isFetching}
              onClick={() => refetch()}
            >
              <RefreshCw className="size-3.5" />
              Reload
            </Button>
          </div>
        ) : data ? (
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2 pb-2">
              <p className="text-sm font-medium">{data.email ?? "Not signed in"}</p>
              <Badge variant={data.admin ? "default" : "secondary"}>
                {data.admin ? "Admin" : "Member"}
              </Badge>
            </div>
            <p className="pb-2 text-xs text-muted-foreground">
              Limits reset on a rolling window.
            </p>
            <div className="divide-y divide-border">
              {ME_LIMIT_ROWS.map(({ key, label }) => (
                <UsageLimitRow key={key} label={label} limit={data.limits[key]} />
              ))}
            </div>
          </div>
        ) : null}
      </div>
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
  const { user, ready } = useSessionUser();

  return (
    <AppLayout
      title="Settings"
      description="Prototype configuration interface"
      actions={
        <Button
          onClick={() =>
            toast("Settings are not persisted in this prototype — forms are for demonstration only.")
          }
        >
          Save changes
        </Button>
      }
    >
      <AccountUsageCard />

      <PrototypeNotice title="Prototype configuration interface" className="mb-4">
        These settings illustrate intended workspace preferences. Changes are not saved to a
        backend in this research prototype.
      </PrototypeNotice>

      <div className="grid gap-4 lg:grid-cols-2">
        <Section
          title="Organization Profile"
          description="Details used across analyses and reports"
          icon={Building2}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="org">Organization name</Label>
              <Input id="org" placeholder="Your organization name" />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="cin">Corporate identity number</Label>
              <Input id="cin" placeholder="Not set" />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="sector">Sector</Label>
              <Input id="sector" placeholder="Your industry sector" />
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
              <Input
                id="contact"
                key={user?.email ?? "pending"}
                defaultValue={ready ? (user?.email ?? "") : undefined}
                readOnly
              />
            </div>
          </div>
        </Section>

        <Section
          title="ESG Framework Preferences"
          description="Frameworks applied during gap analysis"
          icon={Library}
        >
          <div className="space-y-1 divide-y divide-border">
            <ToggleRow label="SEBI BRSR" hint="Active framework · India · 14 prototype requirements" defaultChecked />
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

/**
 * Total requirements for a progress display. A finished analysis reports its own count. A running one
 * reports only what has been persisted so far (results land per model batch), so the framework's
 * requirement count is the real total.
 */
export function resolveRequirementTotal(
  analysisRequirementCount: number,
  frameworkRequirementCount: number | undefined,
  inProgress = false,
): number {
  if (inProgress && frameworkRequirementCount != null && frameworkRequirementCount > 0) {
    return Math.max(frameworkRequirementCount, analysisRequirementCount);
  }
  if (analysisRequirementCount > 0) {
    return analysisRequirementCount;
  }
  if (frameworkRequirementCount != null && frameworkRequirementCount > 0) {
    return frameworkRequirementCount;
  }
  return 0;
}

export function analysisProgressPercent(assessed: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  const percent = Math.round((Math.max(0, assessed) / total) * 100);
  return Math.min(100, percent);
}

/** "X of N requirements assessed", or "X requirements assessed so far" when no total is known. */
export function formatAssessedProgress(assessed: number, total: number): string {
  if (total > 0) {
    return `${assessed} of ${total} requirements assessed`;
  }
  return `${assessed} requirements assessed so far`;
}

/**
 * Elapsed time from startedAt to nowMs.
 * mm:ss under one hour; h:mm:ss at or above one hour.
 */
export function formatElapsedDuration(startedAt: string, nowMs: number): string {
  const startedMs = Date.parse(startedAt);
  if (Number.isNaN(startedMs)) {
    return "0:00";
  }

  const elapsedSeconds = Math.max(0, Math.floor((nowMs - startedMs) / 1000));
  const hours = Math.floor(elapsedSeconds / 3600);
  const minutes = Math.floor((elapsedSeconds % 3600) / 60);
  const seconds = elapsedSeconds % 60;
  const mm = hours > 0 ? String(minutes).padStart(2, "0") : String(minutes);
  const ss = String(seconds).padStart(2, "0");

  if (hours > 0) {
    return `${hours}:${mm}:${ss}`;
  }
  return `${mm}:${ss}`;
}

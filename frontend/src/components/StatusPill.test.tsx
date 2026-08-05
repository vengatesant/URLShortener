import { render, screen } from "@testing-library/react";
import StatusPill from "./StatusPill";

describe("StatusPill", () => {
  it("shows Active for an active, non-expired link", () => {
    render(<StatusPill link={{ active: true, expiresAt: null }} />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("shows Deactivated for an inactive link, even with a future expiry", () => {
    const future = new Date(Date.now() + 86_400_000).toISOString();
    render(<StatusPill link={{ active: false, expiresAt: future }} />);
    expect(screen.getByText("Deactivated")).toBeInTheDocument();
  });

  it("shows Expired for an active link past its expiry", () => {
    const past = new Date(Date.now() - 86_400_000).toISOString();
    render(<StatusPill link={{ active: true, expiresAt: past }} />);
    expect(screen.getByText("Expired")).toBeInTheDocument();
  });
});

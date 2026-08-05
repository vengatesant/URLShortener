import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CreateLinkPage from "./CreateLinkPage";
import { renderWithProviders, mockFetchOnce } from "../test/utils";

describe("CreateLinkPage", () => {
  it("shows the short link after a successful submission", async () => {
    mockFetchOnce(201, {
      shortCode: "abc123",
      shortUrl: "http://localhost:8080/r/abc123",
      longUrl: "https://example.com/a/long/path",
      createdAt: new Date().toISOString(),
      expiresAt: null,
      active: true,
    });

    renderWithProviders(<CreateLinkPage />);

    await userEvent.type(screen.getByLabelText(/long url/i), "https://example.com/a/long/path");
    await userEvent.click(screen.getByRole("button", { name: /shorten/i }));

    expect(await screen.findByText("http://localhost:8080/r/abc123")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /view analytics/i })).toHaveAttribute(
      "href",
      "/links/abc123/stats",
    );
  });

  it("shows the server error message when the alias is taken", async () => {
    mockFetchOnce(409, {
      error: "ALIAS_TAKEN",
      message: "Alias 'taken' is already in use",
      details: [],
      timestamp: new Date().toISOString(),
    });

    renderWithProviders(<CreateLinkPage />);

    await userEvent.type(screen.getByLabelText(/long url/i), "https://example.com");
    await userEvent.type(screen.getByLabelText(/custom alias/i), "taken");
    await userEvent.click(screen.getByRole("button", { name: /shorten/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Alias 'taken' is already in use");
    });
  });
});

import { ApiClientError } from "../api/client";

export default function ErrorNotice({ error }: { error: unknown }) {
  if (!error) return null;

  const message = error instanceof ApiClientError ? error.body.message : "Something went wrong.";
  const details = error instanceof ApiClientError ? error.body.details : [];

  return (
    <div className="notice notice-error" role="alert">
      <p>{message}</p>
      {details.length > 0 && (
        <ul>
          {details.map((detail) => (
            <li key={detail}>{detail}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

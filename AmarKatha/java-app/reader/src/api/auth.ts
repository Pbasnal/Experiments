export type AuthUser = {
  authenticated: true;
  email: string;
  displayName: string;
  role: 'ADMIN' | 'CREATOR' | 'READER';
};

export type AuthMeResponse =
  | { authenticated: false }
  | AuthUser;

export async function fetchMe(): Promise<AuthMeResponse> {
  const res = await fetch('/api/auth/me', { credentials: 'same-origin' });
  if (!res.ok) {
    return { authenticated: false };
  }
  return res.json();
}

export async function logout(): Promise<void> {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
  });
}

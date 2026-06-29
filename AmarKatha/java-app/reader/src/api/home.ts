import type { HomeResponse } from '../types';

export async function fetchHome(): Promise<HomeResponse> {
  const res = await fetch('/api/reader/v1/home');
  if (!res.ok) {
    throw new Error(`Failed to load homepage (${res.status})`);
  }
  return res.json();
}

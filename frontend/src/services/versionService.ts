import axios from 'axios'

/** Shape of the payload returned by the backend version endpoint. */
export interface VersionResponse {
  version: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** Fetches the currently deployed backend application version. */
export async function fetchAppVersion(): Promise<string> {
  const { data } = await axios.get<VersionResponse>(`${apiBaseUrl}/api/version`)
  return data.version
}

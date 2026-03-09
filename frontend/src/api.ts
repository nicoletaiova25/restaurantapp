export class ApiError extends Error {
  status: number;
  isServerError: boolean;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.isServerError = status >= 500;
  }
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

const urlFor = (endpoint: string): string => `${apiBaseUrl}${endpoint}`;

const isJsonResponse = (response: Response): boolean => {
  const contentType = response.headers.get('content-type') ?? '';
  return contentType.includes('application/json');
};

const buildFriendlyHtmlError = (status: number): ApiError => {
  if (status === 404) {
    return new ApiError('Resursa nu a fost gasita (404). Verifica URL-ul sau ID-ul introdus.', status);
  }
  if (status >= 500) {
    return new ApiError('Server error (5xx). Incearca din nou in cateva secunde.', status);
  }
  return new ApiError(
    'Raspuns neasteptat de la server (HTML in loc de JSON). Verifica daca backend-ul ruleaza pe portul corect.',
    status
  );
};

const buildError = async (response: Response): Promise<ApiError> => {
  if (!isJsonResponse(response)) {
    return buildFriendlyHtmlError(response.status);
  }

  try {
    const body = (await response.json()) as { message?: string };
    if (body.message) {
      return new ApiError(body.message, response.status);
    }
  } catch {
    // Keep fallback message below.
  }

  return new ApiError(`${response.status} ${response.statusText}`, response.status);
};

const parseResponse = async <T>(response: Response): Promise<T> => {
  if (!response.ok) {
    throw await buildError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (!isJsonResponse(response)) {
    throw buildFriendlyHtmlError(response.status || 200);
  }

  return (await response.json()) as T;
};

const handleNetworkError = (error: unknown): never => {
  if (error instanceof ApiError) {
    throw error;
  }
  throw new ApiError('Nu ma pot conecta la backend. Porneste Spring Boot si incearca din nou.', 0);
};

export const api = {
  async getAll<T>(endpoint: string): Promise<T[]> {
    try {
      const response = await fetch(urlFor(endpoint));
      return parseResponse<T[]>(response);
    } catch (error) {
      throw handleNetworkError(error);
    }
  },

  async getById<T>(endpoint: string, id: number): Promise<T> {
    try {
      const response = await fetch(`${urlFor(endpoint)}/${id}`);
      return parseResponse<T>(response);
    } catch (error) {
      throw handleNetworkError(error);
    }
  },

  async create<T>(endpoint: string, payload: unknown): Promise<T> {
    try {
      const response = await fetch(urlFor(endpoint), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      return parseResponse<T>(response);
    } catch (error) {
      throw handleNetworkError(error);
    }
  },

  async update<T>(endpoint: string, id: number, payload: unknown): Promise<T> {
    try {
      const response = await fetch(`${urlFor(endpoint)}/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      return parseResponse<T>(response);
    } catch (error) {
      throw handleNetworkError(error);
    }
  },

  async remove(endpoint: string, id: number): Promise<void> {
    try {
      const response = await fetch(`${urlFor(endpoint)}/${id}`, { method: 'DELETE' });
      if (!response.ok) {
        throw await buildError(response);
      }
    } catch (error) {
      throw handleNetworkError(error);
    }
  },
};

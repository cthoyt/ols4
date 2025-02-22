export async function request(
  path: string,
  init?: RequestInit | undefined
): Promise<any> {
  const url = process.env.REACT_APP_APIURL + path;
  const res = await fetch(url.replace(/([^:]\/)\/+/g, "$1"), {
    ...(init ? init : {}),
    //headers: { ...(init?.headers || {}), ...getAuthHeaders() }
  });
  if (!res.ok) {
    const message = `Failure in accesing ${res.url} with status ${res.status} (${res.statusText})`;
    console.dir(message);
    throw new Error(message);
  }
  return await res.json();
}

export class Page<T> {
  constructor(
    public page: number,
    public numElements: number,
    public totalPages: number,
    public totalElements: number,
    public elements: T[],
    public facetFieldsToCounts: Map<string, Map<string, number>>
  ) {}

  map<NewType>(fn: (T) => NewType) {
    return new Page<NewType>(
      this.page,
      this.numElements,
      this.totalPages,
      this.totalElements,
      this.elements.map(fn),
      this.facetFieldsToCounts
    );
  }
}

export async function getPaginated<ResType>(
  path: string
): Promise<Page<ResType>> {
  const res = await get<any>(path);

  return new Page<ResType>(
	res.page || 0,
	res.numElements || 0,
	res.totalPages || 0,
	res.totalElements || 0,
	res.elements || [],
	res.facetFieldsToCounts || new Map()
  );
}

export async function get<ResType>(path: string): Promise<ResType> {
  return request(path);
}

export async function post<ReqType, ResType = any>(
  path: string,
  body: ReqType
): Promise<ResType> {
  return request(path, {
    method: "POST",
    body: JSON.stringify(body),
    headers: {
      "content-type": "application/json",
    },
  });
}

export async function put<ReqType, ResType = any>(
  path: string,
  body: ReqType
): Promise<ResType> {
  return request(path, {
    method: "PUT",
    body: JSON.stringify(body),
    headers: {
      "content-type": "application/json",
    },
  });
}

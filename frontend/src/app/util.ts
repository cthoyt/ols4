import { useEffect, useRef } from "react";

export function asArray<T>(obj: T | T[]): T[] {
  if (Array.isArray(obj)) {
    return obj;
  } else if (obj) {
    return [obj];
  }
  return [];
}

export function randomString() {
  return (Math.random() * Math.pow(2, 54)).toString(36);
}

export function sortByKeys(a: any, b: any) {
  const keyA = a.key.toUpperCase();
  const keyB = b.key.toUpperCase();
  return keyA === keyB ? 0 : keyA > keyB ? 1 : -1;
}

export async function copyToClipboard(text: string) {
  if ("clipboard" in navigator) {
    return await navigator.clipboard.writeText(text);
  } else {
    return document.execCommand("copy", true, text);
  }
}

export function usePrevious(value: any) {
  const ref = useRef();
  useEffect(() => {
    ref.current = value;
  }, [value]);
  return ref.current;
}

import { createContext } from 'react'

export interface Toast {
  id: string
  message: string
  type: 'success' | 'error'
}

export interface ToastContextType {
  toasts: Toast[]
  addToast: (message: string, type?: 'success' | 'error') => void
  removeToast: (id: string) => void
}

export const ToastContext = createContext<ToastContextType | null>(null)

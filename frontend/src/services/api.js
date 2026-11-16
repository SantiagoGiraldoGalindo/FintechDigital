import axios from 'axios'

// URL base de la API REST, configurable con VITE_API_URL (ver .env.example)
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

export default api

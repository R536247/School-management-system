import axios from 'axios'
import { getAccessToken, getSchoolId } from '../utils/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(config => {
  const token = getAccessToken()
  const schoolId = getSchoolId()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (schoolId) {
    config.headers['X-School-Id'] = schoolId
  }
  return config
}, error => Promise.reject(error))

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api

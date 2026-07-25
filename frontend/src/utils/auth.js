export const getAccessToken = () => localStorage.getItem('accessToken')
export const getRefreshToken = () => localStorage.getItem('refreshToken')

export const setTokens = (accessToken, refreshToken) => {
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
}

export const clearTokens = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

export const setSchoolId = (schoolId) => {
  localStorage.setItem('schoolId', schoolId)
}

export const getSchoolId = () => localStorage.getItem('schoolId')

export const isAuthenticated = () => {
  return !!getAccessToken()
}

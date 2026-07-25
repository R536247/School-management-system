import React from 'react'
import { useForm } from 'react-hook-form'
import api from '../services/api'
import { useNavigate } from 'react-router-dom'
import { setTokens, setSchoolId } from '../utils/auth'

export default function Login(){
  const { register, handleSubmit, formState: { errors } } = useForm()
  const nav = useNavigate()
  const [loading, setLoading] = React.useState(false)

  const onSubmit = async (data) => {
    try {
      setLoading(true)
      setSchoolId(data.schoolId)
      const res = await api.post('/auth/login', data, { headers: { 'X-School-Id': data.schoolId } })
      const { accessToken, refreshToken } = res.data
      setTokens(accessToken, refreshToken)
      nav('/students')
    } catch (e) {
      alert(e.response?.data?.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-600 to-blue-800">
      <form className="p-8 bg-white shadow-lg rounded-lg w-96" onSubmit={handleSubmit(onSubmit)}>
        <h1 className="text-3xl font-bold mb-6 text-center text-gray-800">SchoolMS</h1>
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">School ID</label>
          <input 
            type="number"
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500" 
            {...register('schoolId', { required: 'School ID is required' })} 
          />
          {errors.schoolId && <span className="text-red-500 text-sm">{errors.schoolId.message}</span>}
        </div>
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input 
            type="email"
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500" 
            {...register('email', { required: 'Email is required' })} 
          />
          {errors.email && <span className="text-red-500 text-sm">{errors.email.message}</span>}
        </div>
        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
          <input 
            type="password" 
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500" 
            {...register('password', { required: 'Password is required' })} 
          />
          {errors.password && <span className="text-red-500 text-sm">{errors.password.message}</span>}
        </div>
        <button 
          disabled={loading}
          className="w-full bg-blue-600 text-white font-medium px-4 py-3 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? 'Logging in...' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}

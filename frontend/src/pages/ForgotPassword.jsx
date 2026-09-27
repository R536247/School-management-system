import React from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, Link } from 'react-router-dom'
import api from '../services/api'
import { setSchoolId } from '../utils/auth'

export default function ForgotPassword() {
  const { register, handleSubmit, formState: { errors } } = useForm()
  const nav = useNavigate()
  const [loading, setLoading] = React.useState(false)

  const onSubmit = async (data) => {
    if (data.newPassword !== data.confirmPassword) {
      alert('Passordene må være like.')
      return
    }

    try {
      setLoading(true)
      setSchoolId(String(data.schoolId))
      await api.post('/auth/forgot-password', {
        schoolId: Number(data.schoolId),
        email: data.email,
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
        confirmPassword: data.confirmPassword,
      })

      alert('Passordet er oppdatert. Du kan nå logge inn.')
      nav('/login')
    } catch (e) {
      const message = e.response?.data?.message || e.response?.data || 'Kunne ikke oppdatere passordet.'
      alert(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-700 to-slate-900">
      <form className="p-8 bg-white shadow-lg rounded-lg w-full max-w-md" onSubmit={handleSubmit(onSubmit)}>
        <h1 className="text-3xl font-bold mb-2 text-center text-gray-800">Glemt passord</h1>
        <p className="text-sm text-gray-600 text-center mb-6">Bekreft identiteten din med nåværende passord før du oppdaterer.</p>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">School ID</label>
          <input
            type="number"
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500"
            {...register('schoolId', { required: 'School ID er påkrevd' })}
          />
          {errors.schoolId && <span className="text-red-500 text-sm">{errors.schoolId.message}</span>}
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">E-post</label>
          <input
            type="email"
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500"
            {...register('email', { required: 'E-post er påkrevd' })}
          />
          {errors.email && <span className="text-red-500 text-sm">{errors.email.message}</span>}
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">Nåværende passord</label>
          <input
            type="password"
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500"
            {...register('currentPassword', { required: 'Nåværende passord er påkrevd' })}
          />
          {errors.currentPassword && <span className="text-red-500 text-sm">{errors.currentPassword.message}</span>}
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">Nytt passord</label>
          <input
            type="password"
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500"
            {...register('newPassword', { required: 'Nytt passord er påkrevd', minLength: { value: 6, message: 'Minst 6 tegn' } })}
          />
          {errors.newPassword && <span className="text-red-500 text-sm">{errors.newPassword.message}</span>}
        </div>

        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-1">Bekreft nytt passord</label>
          <input
            type="password"
            className="border border-gray-300 p-3 w-full rounded focus:outline-blue-500"
            {...register('confirmPassword', { required: 'Bekreft passordet' })}
          />
          {errors.confirmPassword && <span className="text-red-500 text-sm">{errors.confirmPassword.message}</span>}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 text-white font-medium px-4 py-3 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? 'Oppdaterer...' : 'Oppdater passord'}
        </button>

        <div className="mt-4 text-center text-sm">
          <Link to="/login" className="text-blue-600 hover:underline">Tilbake til innlogging</Link>
        </div>
      </form>
    </div>
  )
}

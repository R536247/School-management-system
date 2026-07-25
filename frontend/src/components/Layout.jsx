import React from 'react'
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom'
import { Menu, LogOut, LayoutDashboard, Users, BookOpen, Clock, FileText } from 'lucide-react'
import { clearTokens } from '../utils/auth'

export default function Layout(){
  const nav = useNavigate()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = React.useState(true)

  const handleLogout = () => {
    clearTokens()
    nav('/login')
  }

  const navItems = [
    { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { path: '/students', icon: Users, label: 'Studenter' },
    { path: '/employees', icon: BookOpen, label: 'Ansatte' },
    { path: '/attendance', icon: Clock, label: 'Frammøte' },
    { path: '/reports', icon: FileText, label: 'Rapporter' },
  ]

  const isActive = (path) => location.pathname === path || (path === '/dashboard' && location.pathname === '/')

  return (
    <div className="min-h-screen flex">
      <aside className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-gray-900 text-white transition-all flex flex-col shadow-lg`}>
        <div className="p-4 flex items-center justify-between border-b border-gray-700">
          {sidebarOpen && <h3 className="text-lg font-bold">SchoolMS</h3>}
          <button onClick={() => setSidebarOpen(!sidebarOpen)} className="p-1 hover:bg-gray-800 rounded">
            <Menu size={20} />
          </button>
        </div>
        <nav className="flex-1 p-4 flex flex-col gap-2">
          {navItems.map((item) => {
            const Icon = item.icon
            const active = isActive(item.path)
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 p-3 rounded transition ${
                  active
                    ? 'bg-blue-600 text-white'
                    : 'hover:bg-gray-800 text-gray-300'
                }`}
              >
                <Icon size={20} />
                {sidebarOpen && <span className="text-sm">{item.label}</span>}
              </Link>
            )
          })}
        </nav>
        <div className="p-4 border-t border-gray-700">
          <button 
            onClick={handleLogout}
            className="flex items-center gap-3 w-full p-3 text-red-400 hover:bg-red-900/20 rounded transition"
          >
            <LogOut size={18} />
            {sidebarOpen && <span className="text-sm">Logg ut</span>}
          </button>
        </div>
      </aside>
      <main className="flex-1 flex flex-col bg-gray-50">
        <header className="bg-white border-b shadow-sm">
          <div className="px-6 py-4 flex items-center justify-between">
            <h1 className="text-xl font-semibold text-gray-800">Skolestyring System</h1>
          </div>
        </header>
        <div className="flex-1 overflow-auto">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

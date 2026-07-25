import { useState, useEffect } from 'react';
import { LogOut, Users, BookOpen, Clock, TrendingUp } from 'lucide-react';
import { Card, CardHeader, CardBody, Loading, Alert } from '../components';
import { Button } from '../components';
import api from '../services/api';
import { isAuthenticated, clearTokens } from '../utils/auth';
import { useNavigate } from 'react-router-dom';
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import Chart from 'chart.js/auto';

export default function Dashboard() {
  const navigate = useNavigate();
  const [dashboardData, setDashboardData] = useState(null);
  const [attendanceData, setAttendanceData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }
    loadDashboard();
  }, [navigate]);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const response = await api.get('/dashboard/summary');
      setDashboardData(response.data);
      
      // Mock attendance data for chart (i real app, ville hente fra backend)
      setAttendanceData({
        labels: ['Man', 'Tirs', 'Ons', 'Tors', 'Fre'],
        datasets: [
          {
            label: 'Tilstede',
            data: [45, 48, 42, 50, 46],
            borderColor: 'rgb(34, 197, 94)',
            backgroundColor: 'rgba(34, 197, 94, 0.1)',
          },
          {
            label: 'Fraværende',
            data: [5, 2, 8, 0, 4],
            borderColor: 'rgb(239, 68, 68)',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
          },
        ],
      });
      
      setError(null);
    } catch (err) {
      setError('Kunne ikke laste dashboard');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    clearTokens();
    navigate('/login');
  };

  if (loading) return <Loading text="Laster dashboard..." />;

  const stats = [
    {
      title: 'Totale studenter',
      value: dashboardData?.totalStudents || 0,
      icon: Users,
      color: 'bg-blue-100 text-blue-600',
    },
    {
      title: 'Totale ansatte',
      value: dashboardData?.totalEmployees || 0,
      icon: BookOpen,
      color: 'bg-green-100 text-green-600',
    },
    {
      title: 'I dag tilstede',
      value: Math.floor((dashboardData?.totalStudents || 0) * 0.92),
      icon: Clock,
      color: 'bg-yellow-100 text-yellow-600',
    },
    {
      title: 'Gj. oppmøte %',
      value: '92%',
      icon: TrendingUp,
      color: 'bg-purple-100 text-purple-600',
    },
  ];

  const StatCard = ({ title, value, icon: Icon, color }) => (
    <Card>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-gray-600 text-sm">{title}</p>
          <p className="text-3xl font-bold text-gray-900 mt-2">{value}</p>
        </div>
        <div className={`${color} p-3 rounded-lg`}>
          <Icon size={24} />
        </div>
      </div>
    </Card>
  );

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <Button onClick={handleLogout} variant="secondary">
          <LogOut size={20} className="inline mr-2" />
          Logg ut
        </Button>
      </div>

      {error && <Alert message={error} type="error" />}

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
        {stats.map((stat, idx) => (
          <StatCard key={idx} {...stat} />
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Line Chart */}
        <Card>
          <CardHeader title="Frammøte denne uka" />
          <CardBody>
            {attendanceData && (
              <Line
                data={attendanceData}
                options={{
                  responsive: true,
                  plugins: {
                    legend: {
                      position: 'bottom',
                    },
                  },
                  scales: {
                    y: {
                      beginAtZero: true,
                    },
                  },
                }}
              />
            )}
          </CardBody>
        </Card>

        {/* Distribution */}
        <Card>
          <CardHeader title="Klassedistribusjon" />
          <CardBody>
            <Doughnut
              data={{
                labels: ['Klasse 10', 'Klasse 11', 'Klasse 12'],
                datasets: [
                  {
                    data: [30, 35, 25],
                    backgroundColor: [
                      'rgba(59, 130, 246, 0.7)',
                      'rgba(34, 197, 94, 0.7)',
                      'rgba(249, 115, 22, 0.7)',
                    ],
                  },
                ],
              }}
              options={{
                responsive: true,
                plugins: {
                  legend: {
                    position: 'bottom',
                  },
                },
              }}
            />
          </CardBody>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader title="Snarveier" />
        <CardBody>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <button
              onClick={() => navigate('/students')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 text-center transition-colors"
            >
              <Users size={24} className="mx-auto mb-2 text-blue-600" />
              <p className="font-medium">Studenter</p>
            </button>
            <button
              onClick={() => navigate('/employees')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 text-center transition-colors"
            >
              <BookOpen size={24} className="mx-auto mb-2 text-green-600" />
              <p className="font-medium">Ansatte</p>
            </button>
            <button
              onClick={() => navigate('/attendance')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 text-center transition-colors"
            >
              <Clock size={24} className="mx-auto mb-2 text-yellow-600" />
              <p className="font-medium">Frammøte</p>
            </button>
            <button
              onClick={() => navigate('/reports')}
              className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 text-center transition-colors"
            >
              <TrendingUp size={24} className="mx-auto mb-2 text-purple-600" />
              <p className="font-medium">Rapporter</p>
            </button>
          </div>
        </CardBody>
      </Card>
    </div>
  );
}

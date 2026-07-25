import { useState, useEffect } from 'react';
import { LogOut, Check, X, Clock } from 'lucide-react';
import { Button, Table, Card, CardHeader, CardBody, Loading, EmptyState, Alert, Badge } from '../components';
import { Select } from '../components/FormInputs';
import api from '../services/api';
import { isAuthenticated, clearTokens } from '../utils/auth';
import { useNavigate } from 'react-router-dom';

export default function Attendance() {
  const navigate = useNavigate();
  const [attendanceRecords, setAttendanceRecords] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    date: new Date().toISOString().split('T')[0],
    entityType: 'student',
  });

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }
    loadData();
  }, [navigate, filters.date]);

  const loadData = async () => {
    try {
      setLoading(true);
      
      // Hent studenter
      const studentsRes = await api.get('/students?page=0&size=100');
      setStudents(studentsRes.data.content || []);
      
      // Hent frammøte for dagen
      const attendanceRes = await api.get(`/attendance?date=${filters.date}`);
      setAttendanceRecords(attendanceRes.data || []);
      
      setError(null);
    } catch (err) {
      setError('Kunne ikke laste data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAttendance = async (entityId, status) => {
    try {
      await api.post('/attendance/mark', {
        entityType: filters.entityType,
        entityId,
        date: filters.date,
        status,
      });
      loadData();
    } catch (err) {
      setError('Kunne ikke markere frammøte');
      console.error(err);
    }
  };

  const handleLogout = () => {
    clearTokens();
    navigate('/login');
  };

  if (loading) return <Loading text="Laster frammøte..." />;

  const statusVariants = {
    present: { icon: Check, color: 'green' },
    absent: { icon: X, color: 'red' },
    late: { icon: Clock, color: 'yellow' },
  };

  const attendanceMap = {};
  attendanceRecords.forEach(record => {
    attendanceMap[record.entityId] = record.status;
  });

  const columns = [
    { key: 'firstName', label: 'Fornavn' },
    { key: 'lastName', label: 'Etternavn' },
    { key: 'admissionNo', label: 'Adm.nr' },
    {
      key: 'id',
      label: 'Frammøte',
      render: (id) => {
        const status = attendanceMap[id];
        if (!status) return '—';
        const Icon = statusVariants[status].icon;
        return <Badge label={status.toUpperCase()} variant={status} size="sm" />;
      },
    },
  ];

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Frammøte</h1>
        <Button onClick={handleLogout} variant="secondary">
          <LogOut size={20} className="inline mr-2" />
          Logg ut
        </Button>
      </div>

      {error && <Alert message={error} type="error" />}

      {/* Filters */}
      <Card className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Dato</label>
            <input
              type="date"
              value={filters.date}
              onChange={(e) => setFilters({ ...filters, date: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Type</label>
            <Select
              value={filters.entityType}
              onChange={(e) => setFilters({ ...filters, entityType: e.target.value })}
              options={[
                { value: 'student', label: 'Studenter' },
                { value: 'employee', label: 'Ansatte' },
              ]}
            />
          </div>
        </div>
      </Card>

      {/* Attendance Records */}
      <Card>
        <CardHeader
          title={`Frammøte ${filters.date}`}
          subtitle={`${filters.entityType === 'student' ? 'Studenter' : 'Ansatte'}`}
        />
        <CardBody>
          <div className="space-y-3">
            {students.map((student) => (
              <div
                key={student.id}
                className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50"
              >
                <div>
                  <p className="font-medium text-gray-900">
                    {student.firstName} {student.lastName}
                  </p>
                  <p className="text-sm text-gray-600">{student.admissionNo}</p>
                </div>
                <div className="flex gap-2">
                  <Button
                    onClick={() => handleMarkAttendance(student.id, 'present')}
                    variant={attendanceMap[student.id] === 'present' ? 'success' : 'secondary'}
                    size="sm"
                  >
                    <Check size={18} />
                  </Button>
                  <Button
                    onClick={() => handleMarkAttendance(student.id, 'late')}
                    variant={attendanceMap[student.id] === 'late' ? 'warning' : 'secondary'}
                    size="sm"
                  >
                    <Clock size={18} />
                  </Button>
                  <Button
                    onClick={() => handleMarkAttendance(student.id, 'absent')}
                    variant={attendanceMap[student.id] === 'absent' ? 'danger' : 'secondary'}
                    size="sm"
                  >
                    <X size={18} />
                  </Button>
                </div>
              </div>
            ))}
          </div>
          {students.length === 0 && (
            <EmptyState
              title="Ingen studenter"
              description="Det finnes ingen studenter til denne datoen"
            />
          )}
        </CardBody>
      </Card>
    </div>
  );
}

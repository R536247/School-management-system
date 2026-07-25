import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, LogOut } from 'lucide-react';
import { Button, Table, Modal, Card, Loading, Alert, Badge } from '../components';
import { Input, Select } from '../components/FormInputs';
import api from '../services/api';
import { isAuthenticated, clearTokens } from '../utils/auth';
import { useNavigate } from 'react-router-dom';

export default function Students() {
  const navigate = useNavigate();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    admissionNo: '',
    dob: '',
    gender: '',
    status: 'active',
  });

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }
    fetchStudents();
  }, [navigate]);

  const fetchStudents = async () => {
    try {
      setLoading(true);
      const response = await api.get('/students?page=0&size=50');
      setStudents(response.data.content || []);
      setError(null);
    } catch (err) {
      setError('Kunne ikke laste studenter');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (student = null) => {
    if (student) {
      setEditingId(student.id);
      setFormData({
        firstName: student.firstName,
        lastName: student.lastName,
        admissionNo: student.admissionNo || '',
        dob: student.dob || '',
        gender: student.gender || '',
        status: student.status,
      });
    } else {
      setEditingId(null);
      setFormData({
        firstName: '',
        lastName: '',
        admissionNo: '',
        dob: '',
        gender: '',
        status: 'active',
      });
    }
    setIsModalOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editingId) {
        await api.put(`/students/${editingId}`, formData);
      } else {
        await api.post('/students', formData);
      }
      fetchStudents();
      setIsModalOpen(false);
      setError(null);
    } catch (err) {
      const message = err.response?.data || 'Kunne ikke lagre student';
      setError(typeof message === 'string' ? message : 'Kunne ikke lagre student');
      console.error(err);
    }
  };

  const handleDelete = async (id) => {
    if (confirm('Er du sikker på at du vil slette?')) {
      try {
        await api.delete(`/students/${id}`);
        fetchStudents();
      } catch (err) {
        setError('Kunne ikke slette student');
        console.error(err);
      }
    }
  };

  const handleLogout = () => {
    clearTokens();
    navigate('/login');
  };

  if (loading) return <Loading text="Laster studenter..." />;

  const columns = [
    { key: 'firstName', label: 'Fornavn' },
    { key: 'lastName', label: 'Etternavn' },
    { key: 'admissionNo', label: 'Adm.nr' },
    { key: 'gender', label: 'Kjønn' },
    { key: 'status', label: 'Status', render: (val) => <Badge label={val} variant={val === 'active' ? 'success' : 'danger'} size="sm" /> },
  ];

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Studenter</h1>
        <div className="flex gap-2">
          <Button onClick={() => handleOpenModal()} variant="primary">
            <Plus size={20} className="inline mr-2" />
            Legg til student
          </Button>
          <Button onClick={handleLogout} variant="secondary">
            <LogOut size={20} className="inline mr-2" />
            Logg ut
          </Button>
        </div>
      </div>

      {error && <Alert message={error} type="error" />}

      <Card>
        <Table
          columns={columns}
          data={students}
          searchableFields={['firstName', 'lastName', 'admissionNo']}
          itemsPerPage={10}
          actions={(student) => (
            <>
              <Button
                onClick={() => handleOpenModal(student)}
                variant="ghost"
                size="sm"
              >
                <Edit size={18} />
              </Button>
              <Button
                onClick={() => handleDelete(student.id)}
                variant="danger"
                size="sm"
              >
                <Trash2 size={18} />
              </Button>
            </>
          )}
        />
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingId ? 'Rediger student' : 'Legg til student'}
        footer={
          <>
            <Button onClick={() => setIsModalOpen(false)} variant="secondary">
              Avbryt
            </Button>
            <Button onClick={handleSave} variant="primary">
              {editingId ? 'Oppdater' : 'Lagre'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input
            label="Fornavn"
            value={formData.firstName}
            onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
            required
          />
          <Input
            label="Etternavn"
            value={formData.lastName}
            onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
            required
          />
          <Input
            label="Adm.nr"
            value={formData.admissionNo}
            onChange={(e) => setFormData({ ...formData, admissionNo: e.target.value })}
          />
          <Input
            label="Fødselsdato"
            type="date"
            value={formData.dob}
            onChange={(e) => setFormData({ ...formData, dob: e.target.value })}
          />
          <Select
            label="Kjønn"
            value={formData.gender}
            onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
            options={[
              { value: 'M', label: 'Mann' },
              { value: 'F', label: 'Kvinne' },
              { value: 'Other', label: 'Annet' },
            ]}
          />
          <Select
            label="Status"
            value={formData.status}
            onChange={(e) => setFormData({ ...formData, status: e.target.value })}
            options={[
              { value: 'active', label: 'Aktiv' },
              { value: 'inactive', label: 'Inaktiv' },
            ]}
          />
        </div>
      </Modal>
    </div>
  );
}


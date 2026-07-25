import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, LogOut } from 'lucide-react';
import { Button, Table, Modal, Card, CardHeader, CardBody, CardFooter, Loading, EmptyState, Alert } from '../components';
import { Input, Select } from '../components/FormInputs';
import api from '../services/api';
import { isAuthenticated, clearTokens } from '../utils/auth';
import { useNavigate } from 'react-router-dom';

export default function Employees() {
  const navigate = useNavigate();
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    roleTitle: '',
    department: '',
    hireDate: '',
    status: 'active',
  });

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate('/login');
      return;
    }
    fetchEmployees();
  }, [navigate]);

  const fetchEmployees = async () => {
    try {
      setLoading(true);
      const response = await api.get('/employees?page=0&size=50');
      setEmployees(response.data.content || []);
      setError(null);
    } catch (err) {
      setError('Kunne ikke laste ansatte');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (employee = null) => {
    if (employee) {
      setEditingId(employee.id);
      setFormData({
        firstName: employee.firstName,
        lastName: employee.lastName,
        roleTitle: employee.roleTitle,
        department: employee.department,
        hireDate: employee.hireDate || '',
        status: employee.status,
      });
    } else {
      setEditingId(null);
      setFormData({
        firstName: '',
        lastName: '',
        roleTitle: '',
        department: '',
        hireDate: '',
        status: 'active',
      });
    }
    setIsModalOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editingId) {
        await api.put(`/employees/${editingId}`, formData);
      } else {
        await api.post('/employees', formData);
      }
      fetchEmployees();
      setIsModalOpen(false);
      setError(null);
    } catch (err) {
      setError('Kunne ikke lagre ansatt');
      console.error(err);
    }
  };

  const handleDelete = async (id) => {
    if (confirm('Er du sikker på at du vil slette?')) {
      try {
        await api.delete(`/employees/${id}`);
        fetchEmployees();
      } catch (err) {
        setError('Kunne ikke slette ansatt');
        console.error(err);
      }
    }
  };

  const handleLogout = () => {
    clearTokens();
    navigate('/login');
  };

  if (loading) return <Loading text="Laster ansatte..." />;

  const columns = [
    { key: 'firstName', label: 'Fornavn' },
    { key: 'lastName', label: 'Etternavn' },
    { key: 'roleTitle', label: 'Rolle' },
    { key: 'department', label: 'Avdeling' },
    { key: 'status', label: 'Status', render: (val) => <span className="capitalize">{val}</span> },
  ];

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Ansatte</h1>
        <div className="flex gap-2">
          <Button onClick={() => handleOpenModal()} variant="primary">
            <Plus size={20} className="inline mr-2" />
            Legg til ansatt
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
          data={employees}
          searchableFields={['firstName', 'lastName', 'department']}
          itemsPerPage={10}
          actions={(employee) => (
            <>
              <Button
                onClick={() => handleOpenModal(employee)}
                variant="ghost"
                size="sm"
              >
                <Edit size={18} />
              </Button>
              <Button
                onClick={() => handleDelete(employee.id)}
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
        title={editingId ? 'Rediger ansatt' : 'Legg til ansatt'}
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
            label="Rolle"
            value={formData.roleTitle}
            onChange={(e) => setFormData({ ...formData, roleTitle: e.target.value })}
          />
          <Input
            label="Avdeling"
            value={formData.department}
            onChange={(e) => setFormData({ ...formData, department: e.target.value })}
          />
          <Input
            label="Ansettelsesdato"
            type="date"
            value={formData.hireDate}
            onChange={(e) => setFormData({ ...formData, hireDate: e.target.value })}
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

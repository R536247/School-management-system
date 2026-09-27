import { useState } from 'react';
import { LogOut, Download, Filter } from 'lucide-react';
import { Button, Card, CardHeader, CardBody, Loading, EmptyState, Alert, Badge } from '../components';
import { Select, Input } from '../components/FormInputs';
import api from '../services/api';
import { isAuthenticated, clearTokens } from '../utils/auth';
import { useNavigate } from 'react-router-dom';

export default function Reports() {
  const navigate = useNavigate();
  const [reportType, setReportType] = useState('attendance');
  const [dateRange, setDateRange] = useState({
    startDate: '',
    endDate: '',
  });
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const generateReport = async () => {
    if (!dateRange.startDate || !dateRange.endDate) {
      setError('Vennligst velg dato fra og til');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const response = await api.get('/reports', {
        params: { type: reportType, startDate: dateRange.startDate, endDate: dateRange.endDate },
      });
      setReports(response.data || []);
    } catch (err) {
      setError('Kunne ikke generere rapport');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = () => {
    const csv = generateCSV();
    downloadCSV(csv);
  };

  const generateCSV = () => {
    if (!reports.length) return '';

    const headers = Object.keys(reports[0]).join(',');
    const rows = reports.map(report =>
      Object.values(report).join(',')
    );

    return [headers, ...rows].join('\n');
  };

  const downloadCSV = (csv) => {
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv));
    element.setAttribute('download', `rapport-${reportType}-${new Date().toISOString()}.csv`);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  const handleLogout = () => {
    clearTokens();
    navigate('/login');
  };

  const reportTitles = {
    attendance: 'Frammøterapport',
    students: 'Studentrapport',
    performance: 'Ytelsesrapport',
  };

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Rapporter</h1>
        <Button onClick={handleLogout} variant="secondary">
          <LogOut size={20} className="inline mr-2" />
          Logg ut
        </Button>
      </div>

      {error && <Alert message={error} type="error" />}

      {/* Filters */}
      <Card className="mb-6">
        <CardHeader title="Generer rapport" />
        <CardBody>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Select
              label="Rapporttype"
              value={reportType}
              onChange={(e) => {
                setReportType(e.target.value);
                setReports([]);
              }}
              options={[
                { value: 'attendance', label: 'Frammøte' },
                { value: 'students', label: 'Studenter' },
                { value: 'performance', label: 'Ytelse' },
              ]}
            />
            <Input
              label="Fra dato"
              type="date"
              value={dateRange.startDate}
              onChange={(e) => setDateRange({ ...dateRange, startDate: e.target.value })}
            />
            <Input
              label="Til dato"
              type="date"
              value={dateRange.endDate}
              onChange={(e) => setDateRange({ ...dateRange, endDate: e.target.value })}
            />
            <div className="flex items-end">
              <Button
                onClick={generateReport}
                variant="primary"
                disabled={loading}
              >
                <Filter size={20} className="inline mr-2" />
                Generer
              </Button>
            </div>
          </div>
        </CardBody>
      </Card>

      {/* Report Results */}
      {loading ? (
        <Loading text="Genererer rapport..." />
      ) : reports.length > 0 ? (
        <Card>
          <CardHeader
            title={reportTitles[reportType]}
            action={
              reports.length > 0 && (
                <Button onClick={handleExport} variant="ghost">
                  <Download size={20} className="inline mr-2" />
                  Eksporter
                </Button>
              )
            }
          />
          <CardBody>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead className="border-b border-gray-200">
                  <tr>
                    {Object.keys(reports[0]).map((key) => (
                      <th
                        key={key}
                        className="px-4 py-2 font-semibold text-gray-900 capitalize"
                      >
                        {key.replace(/([A-Z])/g, ' $1')}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {reports.map((report, idx) => (
                    <tr key={idx} className="border-b border-gray-100 hover:bg-gray-50">
                      {Object.entries(report).map(([key, value]) => (
                        <td key={key} className="px-4 py-3 text-gray-900">
                          {key === 'status' ? (
                            <Badge
                              label={value}
                              variant={value === 'active' ? 'success' : 'danger'}
                              size="sm"
                            />
                          ) : typeof value === 'number' ? (
                            <span className="font-medium">{value}</span>
                          ) : (
                            value
                          )}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardBody>
        </Card>
      ) : (
        <Card>
          <EmptyState
            title="Ingen rapport generert"
            description="Velg rapporttype og datoer for å generere en rapport"
          />
        </Card>
      )}
    </div>
  );
}

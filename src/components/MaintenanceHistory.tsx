import React, { useState } from 'react';
import {
  Wrench,
  Plus,
  Trash2,
  X,
  CheckCircle2,
  Filter,
  DollarSign,
  Building2,
  Calendar,
} from 'lucide-react';
import { Maintenance, Vehicle } from '../types';

interface MaintenanceHistoryProps {
  maintenance: Maintenance[];
  vehicles: Vehicle[];
  onSaveMaintenance: (item: Maintenance) => void;
  onDeleteMaintenance: (id: number) => void;
  initialVehicleFilter?: string;
  initialAddOpen?: boolean;
}

export const MaintenanceHistory: React.FC<MaintenanceHistoryProps> = ({
  maintenance,
  vehicles,
  onSaveMaintenance,
  onDeleteMaintenance,
  initialVehicleFilter = '',
  initialAddOpen = false,
}) => {
  const [selectedVehicleFilter, setSelectedVehicleFilter] =
    useState(initialVehicleFilter);
  const [isFormOpen, setIsFormOpen] = useState(initialAddOpen);

  const [formData, setFormData] = useState({
    vehicleName: vehicles[0]?.vehicleName || '',
    serviceTitle: '',
    serviceType: 'Oil Change',
    serviceDate: new Date().toISOString().split('T')[0],
    currentOdometer: vehicles[0]?.odometerReading || '0',
    serviceCost: '0.00',
    workshopName: '',
    notes: '',
  });

  const filteredMaintenance = selectedVehicleFilter
    ? maintenance.filter(
        (m) =>
          m.vehicleName.toLowerCase() === selectedVehicleFilter.toLowerCase()
      )
    : maintenance;

  const totalServiceCost = filteredMaintenance.reduce(
    (acc, m) => acc + (parseFloat(m.serviceCost) || 0),
    0
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.vehicleName.trim() || !formData.serviceTitle.trim()) {
      alert('Please fill in required fields: Vehicle Name and Service Title.');
      return;
    }

    const newItem: Maintenance = {
      id: Date.now(),
      vehicleName: formData.vehicleName.trim(),
      serviceTitle: formData.serviceTitle.trim(),
      serviceType: formData.serviceType,
      serviceDate: formData.serviceDate,
      currentOdometer: formData.currentOdometer.trim(),
      serviceCost: formData.serviceCost.trim(),
      workshopName: formData.workshopName.trim(),
      notes: formData.notes.trim(),
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    onSaveMaintenance(newItem);
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Wrench className="w-6 h-6 text-purple-600" />
            <span>
              {selectedVehicleFilter
                ? `${selectedVehicleFilter} - Service History`
                : 'Maintenance History'}
            </span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Total Maintenance Records: {filteredMaintenance.length}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          {/* Vehicle Filter */}
          <div className="flex items-center gap-1.5 bg-slate-50 px-3 py-1.5 border border-slate-200 rounded-xl text-xs">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <select
              value={selectedVehicleFilter}
              onChange={(e) => setSelectedVehicleFilter(e.target.value)}
              className="bg-transparent text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
            >
              <option value="">All Vehicles ({maintenance.length})</option>
              {vehicles.map((v) => (
                <option key={v.id} value={v.vehicleName}>
                  {v.vehicleName}
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={() => {
              if (vehicles.length > 0 && !formData.vehicleName) {
                setFormData((prev) => ({
                  ...prev,
                  vehicleName: vehicles[0].vehicleName,
                }));
              }
              setIsFormOpen(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-bold shadow transition-all whitespace-nowrap"
          >
            <Plus className="w-4 h-4" />
            <span>Add Maintenance</span>
          </button>
        </div>
      </div>

      {/* Stats Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm">
          <span className="text-[10px] font-bold uppercase text-slate-400 block">
            Total Service Investment
          </span>
          <div className="mt-1 flex items-baseline gap-1">
            <span className="text-2xl font-extrabold text-slate-900">
              ${totalServiceCost.toFixed(2)}
            </span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm">
          <span className="text-[10px] font-bold uppercase text-slate-400 block">
            Avg Service Expense
          </span>
          <div className="mt-1 flex items-baseline gap-1">
            <span className="text-2xl font-extrabold text-purple-600">
              ${filteredMaintenance.length > 0
                ? (totalServiceCost / filteredMaintenance.length).toFixed(2)
                : '0.00'}
            </span>
            <span className="text-xs font-semibold text-slate-500">per record</span>
          </div>
        </div>
      </div>

      {/* Maintenance Cards List */}
      {filteredMaintenance.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm">
          <Wrench className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">
            No Maintenance Records Added Yet.
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Log your vehicle servicing, oil changes, brake repairs, and workshop invoices.
          </p>
          <button
            onClick={() => setIsFormOpen(true)}
            className="mt-4 px-4 py-2 bg-purple-600 text-white rounded-xl text-xs font-bold shadow hover:bg-purple-500 transition-all"
          >
            Log Maintenance Service
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredMaintenance.map((m) => (
            <div
              key={m.id}
              className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow transition-all flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-extrabold text-slate-900">
                    {m.serviceTitle}
                  </span>
                  <span className="bg-purple-50 text-purple-700 text-xs font-semibold px-2.5 py-0.5 rounded-full border border-purple-200">
                    {m.serviceType}
                  </span>
                  <span className="bg-slate-100 text-slate-700 text-xs font-medium px-2 py-0.5 rounded">
                    {m.vehicleName}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-x-4 gap-y-1 text-xs text-slate-600 pt-1">
                  <div>
                    <span className="text-slate-400">Date:</span>{' '}
                    <span className="font-semibold text-slate-800">{m.serviceDate}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Odometer:</span>{' '}
                    <span className="font-semibold text-slate-800">{m.currentOdometer}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Cost:</span>{' '}
                    <span className="font-bold text-purple-600">${m.serviceCost}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Workshop:</span>{' '}
                    <span className="font-semibold text-slate-800">
                      {m.workshopName || 'N/A'}
                    </span>
                  </div>
                </div>

                {m.notes && (
                  <p className="text-xs text-slate-500 italic mt-1">Notes: {m.notes}</p>
                )}
              </div>

              <div className="self-end md:self-center">
                <button
                  onClick={() => {
                    if (window.confirm('Delete this maintenance record?')) {
                      onDeleteMaintenance(m.id);
                    }
                  }}
                  className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all"
                  title="Delete Record"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Form Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
              <h2 className="text-lg font-extrabold flex items-center gap-2">
                <Wrench className="w-5 h-5 text-purple-400" />
                <span>Add Maintenance Service</span>
              </h2>
              <button
                onClick={() => setIsFormOpen(false)}
                className="p-1.5 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs font-semibold text-slate-700">
                <div className="sm:col-span-2">
                  <label className="block mb-1">Vehicle Name *</label>
                  {vehicles.length > 0 ? (
                    <select
                      value={formData.vehicleName}
                      onChange={(e) =>
                        setFormData({ ...formData, vehicleName: e.target.value })
                      }
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                    >
                      {vehicles.map((v) => (
                        <option key={v.id} value={v.vehicleName}>
                          {v.vehicleName}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type="text"
                      required
                      placeholder="Enter vehicle name"
                      value={formData.vehicleName}
                      onChange={(e) =>
                        setFormData({ ...formData, vehicleName: e.target.value })
                      }
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                    />
                  )}
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Service Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Synthetic Oil & Filter Change"
                    value={formData.serviceTitle}
                    onChange={(e) =>
                      setFormData({ ...formData, serviceTitle: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Service Type</label>
                  <select
                    value={formData.serviceType}
                    onChange={(e) =>
                      setFormData({ ...formData, serviceType: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                  >
                    <option value="Oil Change">Oil Change</option>
                    <option value="Brake Service">Brake Service</option>
                    <option value="Tire Rotation / Replacement">Tire Service</option>
                    <option value="Engine Tuning">Engine Tuning</option>
                    <option value="General Inspection">General Inspection</option>
                    <option value="Battery Replacement">Battery Service</option>
                    <option value="Transmission Service">Transmission</option>
                    <option value="Custom Repair">Custom Repair</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1">Service Date</label>
                  <input
                    type="date"
                    required
                    value={formData.serviceDate}
                    onChange={(e) =>
                      setFormData({ ...formData, serviceDate: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Current Odometer</label>
                  <input
                    type="number"
                    placeholder="e.g. 28450"
                    value={formData.currentOdometer}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        currentOdometer: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Service Cost ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="e.g. 89.99"
                    value={formData.serviceCost}
                    onChange={(e) =>
                      setFormData({ ...formData, serviceCost: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Workshop Name</label>
                  <input
                    type="text"
                    placeholder="e.g. Toyota Certified Care Center"
                    value={formData.workshopName}
                    onChange={(e) =>
                      setFormData({ ...formData, workshopName: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Notes</label>
                  <textarea
                    rows={2}
                    placeholder="Details about parts replaced or repairs..."
                    value={formData.notes}
                    onChange={(e) =>
                      setFormData({ ...formData, notes: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
                  />
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsFormOpen(false)}
                  className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex items-center gap-1.5 px-5 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-bold shadow transition-all"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Save Maintenance Record</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

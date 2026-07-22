import React, { useState } from 'react';
import {
  BellRing,
  Plus,
  Trash2,
  X,
  CheckCircle2,
  Filter,
  Calendar,
  AlertCircle,
  Clock,
} from 'lucide-react';
import { Reminder, Vehicle } from '../types';
import { ReminderUtils } from '../utils/calculations';

interface ServiceRemindersProps {
  reminders: Reminder[];
  vehicles: Vehicle[];
  onSaveReminder: (reminder: Reminder) => void;
  onDeleteReminder: (id: number) => void;
  initialVehicleFilter?: string;
  initialAddOpen?: boolean;
}

export const ServiceReminders: React.FC<ServiceRemindersProps> = ({
  reminders,
  vehicles,
  onSaveReminder,
  onDeleteReminder,
  initialVehicleFilter = '',
  initialAddOpen = false,
}) => {
  const [selectedVehicleFilter, setSelectedVehicleFilter] =
    useState(initialVehicleFilter);
  const [isFormOpen, setIsFormOpen] = useState(initialAddOpen);

  const [formData, setFormData] = useState({
    vehicleName: vehicles[0]?.vehicleName || '',
    reminderTitle: '',
    reminderType: 'Service Due',
    dueDate: new Date(Date.now() + 30 * 24 * 3600 * 1000)
      .toISOString()
      .split('T')[0],
    currentOdometer: vehicles[0]?.odometerReading || '0',
    nextServiceOdometer: '30000',
    notes: '',
  });

  const filteredReminders = selectedVehicleFilter
    ? reminders.filter(
        (r) =>
          r.vehicleName.toLowerCase() === selectedVehicleFilter.toLowerCase()
      )
    : reminders;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.vehicleName.trim() || !formData.reminderTitle.trim()) {
      alert('Please fill in required fields: Vehicle Name and Reminder Title.');
      return;
    }

    const calculatedStatus = ReminderUtils.calculateStatus(formData.dueDate);

    const newReminder: Reminder = {
      id: Date.now(),
      vehicleName: formData.vehicleName.trim(),
      reminderTitle: formData.reminderTitle.trim(),
      reminderType: formData.reminderType,
      dueDate: formData.dueDate,
      currentOdometer: formData.currentOdometer.trim(),
      nextServiceOdometer: formData.nextServiceOdometer.trim(),
      notes: formData.notes.trim(),
      status: calculatedStatus,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    onSaveReminder(newReminder);
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <BellRing className="w-6 h-6 text-amber-600" />
            <span>
              {selectedVehicleFilter
                ? `${selectedVehicleFilter} - Reminders`
                : 'Service Reminders'}
            </span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Total Active Reminders: {filteredReminders.length}
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
              <option value="">All Vehicles ({reminders.length})</option>
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
            className="flex items-center gap-2 px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white rounded-xl text-xs font-bold shadow transition-all whitespace-nowrap"
          >
            <Plus className="w-4 h-4" />
            <span>Add Reminder</span>
          </button>
        </div>
      </div>

      {/* Reminders List */}
      {filteredReminders.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm">
          <BellRing className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">
            No Reminders Added Yet.
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Never miss an oil change, tire check, or insurance renewal again.
          </p>
          <button
            onClick={() => setIsFormOpen(true)}
            className="mt-4 px-4 py-2 bg-amber-600 text-white rounded-xl text-xs font-bold shadow hover:bg-amber-500 transition-all"
          >
            Create Service Reminder
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredReminders.map((r) => {
            const statusStr = ReminderUtils.calculateStatus(r.dueDate);
            const isOverdue = statusStr === 'Overdue';
            const isDueToday = statusStr === 'Due Today';

            return (
              <div
                key={r.id}
                className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                        {r.reminderType}
                      </span>
                      <h3 className="text-base font-extrabold text-slate-900 mt-1">
                        {r.reminderTitle}
                      </h3>
                      <p className="text-xs font-semibold text-blue-600 mt-0.5">
                        {r.vehicleName}
                      </p>
                    </div>

                    <span
                      className={`text-xs font-extrabold px-3 py-1 rounded-xl shadow-xs whitespace-nowrap ${
                        isOverdue
                          ? 'bg-red-100 text-red-800 border border-red-200'
                          : isDueToday
                          ? 'bg-amber-100 text-amber-900 border border-amber-200'
                          : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                      }`}
                    >
                      {statusStr}
                    </span>
                  </div>

                  <div className="mt-4 pt-3 border-t border-slate-100 grid grid-cols-2 gap-2 text-xs text-slate-600">
                    <div>
                      <span className="text-slate-400 block font-medium">Due Date:</span>
                      <span className="font-bold text-slate-800 flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-amber-600" />
                        {r.dueDate}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block font-medium">Target Odometer:</span>
                      <span className="font-semibold text-slate-800">
                        {r.nextServiceOdometer ? `${r.nextServiceOdometer} km` : 'N/A'}
                      </span>
                    </div>
                  </div>

                  {r.notes && (
                    <p className="text-xs text-slate-500 italic mt-3 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                      Notes: {r.notes}
                    </p>
                  )}
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-end">
                  <button
                    onClick={() => {
                      if (window.confirm('Delete or mark this reminder as completed?')) {
                        onDeleteReminder(r.id);
                      }
                    }}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-xl text-xs font-bold transition-all"
                  >
                    <Trash2 className="w-4 h-4" />
                    <span>Dismiss / Delete</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add Form Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
              <h2 className="text-lg font-extrabold flex items-center gap-2">
                <BellRing className="w-5 h-5 text-amber-400" />
                <span>Add Service Reminder</span>
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
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
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
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
                    />
                  )}
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Reminder Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. 30,000 KM Major Service / Oil Check"
                    value={formData.reminderTitle}
                    onChange={(e) =>
                      setFormData({ ...formData, reminderTitle: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Reminder Type</label>
                  <select
                    value={formData.reminderType}
                    onChange={(e) =>
                      setFormData({ ...formData, reminderType: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
                  >
                    <option value="Service Due">Service Due</option>
                    <option value="Insurance Renewal">Insurance Renewal</option>
                    <option value="Tax & Registration">Tax & Registration</option>
                    <option value="Tire Check">Tire Inspection</option>
                    <option value="Custom">Custom Reminder</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1">Due Date *</label>
                  <input
                    type="date"
                    required
                    value={formData.dueDate}
                    onChange={(e) =>
                      setFormData({ ...formData, dueDate: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
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
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Next Service Odometer</label>
                  <input
                    type="number"
                    placeholder="e.g. 30000"
                    value={formData.nextServiceOdometer}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        nextServiceOdometer: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Notes</label>
                  <textarea
                    rows={2}
                    placeholder="Details about items to inspect or replace..."
                    value={formData.notes}
                    onChange={(e) =>
                      setFormData({ ...formData, notes: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
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
                  className="flex items-center gap-1.5 px-5 py-2 bg-amber-600 hover:bg-amber-500 text-white rounded-xl text-xs font-bold shadow transition-all"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Save Service Reminder</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

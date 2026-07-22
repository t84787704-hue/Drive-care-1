import React, { useState } from 'react';
import {
  Fuel,
  Plus,
  Trash2,
  X,
  CheckCircle2,
  Filter,
  DollarSign,
  Gauge,
  TrendingUp,
} from 'lucide-react';
import { FuelEntry, Vehicle } from '../types';
import { FuelReportUtils, MileageUtils } from '../utils/calculations';

interface FuelTrackerProps {
  fuelEntries: FuelEntry[];
  vehicles: Vehicle[];
  onSaveFuelEntry: (entry: FuelEntry) => void;
  onDeleteFuelEntry: (id: number) => void;
  initialVehicleFilter?: string;
  initialAddOpen?: boolean;
}

export const FuelTracker: React.FC<FuelTrackerProps> = ({
  fuelEntries,
  vehicles,
  onSaveFuelEntry,
  onDeleteFuelEntry,
  initialVehicleFilter = '',
  initialAddOpen = false,
}) => {
  const [selectedVehicleFilter, setSelectedVehicleFilter] =
    useState(initialVehicleFilter);
  const [isFormOpen, setIsFormOpen] = useState(initialAddOpen);

  const [formData, setFormData] = useState({
    vehicleName: vehicles[0]?.vehicleName || '',
    fuelDate: new Date().toISOString().split('T')[0],
    fuelType: 'Petrol',
    fuelQuantity: '40',
    amountPaid: '60.00',
    currentOdometer: vehicles[0]?.odometerReading || '0',
    fuelStationName: 'Shell Express',
    notes: '',
  });

  const filteredEntries = selectedVehicleFilter
    ? fuelEntries.filter(
        (f) =>
          f.vehicleName.toLowerCase() === selectedVehicleFilter.toLowerCase()
      )
    : fuelEntries;

  // Stats
  const totalCost = FuelReportUtils.getTotalFuelCost(
    filteredEntries.map((f) => parseFloat(f.amountPaid) || 0)
  );

  const totalQuantity = FuelReportUtils.getTotalFuelQuantity(
    filteredEntries.map((f) => parseFloat(f.fuelQuantity) || 0)
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.vehicleName.trim()) {
      alert('Please select or enter a vehicle name.');
      return;
    }

    const newEntry: FuelEntry = {
      id: Date.now(),
      vehicleName: formData.vehicleName.trim(),
      fuelDate: formData.fuelDate,
      fuelType: formData.fuelType,
      fuelQuantity: formData.fuelQuantity.trim(),
      amountPaid: formData.amountPaid.trim(),
      currentOdometer: formData.currentOdometer.trim(),
      fuelStationName: formData.fuelStationName.trim(),
      notes: formData.notes.trim(),
      createdAt: Date.now(),
    };

    onSaveFuelEntry(newEntry);
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-6">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Fuel className="w-6 h-6 text-emerald-600" />
            <span>
              {selectedVehicleFilter
                ? `${selectedVehicleFilter} - Fuel History`
                : 'Fuel Tracker'}
            </span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Total Fuel Entries: {filteredEntries.length}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          {/* Vehicle Filter dropdown */}
          <div className="flex items-center gap-1.5 bg-slate-50 px-3 py-1.5 border border-slate-200 rounded-xl text-xs">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <select
              value={selectedVehicleFilter}
              onChange={(e) => setSelectedVehicleFilter(e.target.value)}
              className="bg-transparent text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
            >
              <option value="">All Vehicles ({fuelEntries.length})</option>
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
            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow transition-all whitespace-nowrap"
          >
            <Plus className="w-4 h-4" />
            <span>Log Fuel Fill-Up</span>
          </button>
        </div>
      </div>

      {/* Fuel Statistics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm">
          <span className="text-[10px] font-bold uppercase text-slate-400 block">
            Total Fuel Cost
          </span>
          <div className="mt-1 flex items-baseline gap-1">
            <span className="text-2xl font-extrabold text-slate-900">
              ${totalCost.toFixed(2)}
            </span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm">
          <span className="text-[10px] font-bold uppercase text-slate-400 block">
            Total Fuel Volume
          </span>
          <div className="mt-1 flex items-baseline gap-1">
            <span className="text-2xl font-extrabold text-slate-900">
              {totalQuantity.toFixed(1)}
            </span>
            <span className="text-xs font-semibold text-slate-500">Liters</span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm">
          <span className="text-[10px] font-bold uppercase text-slate-400 block">
            Avg Fuel Logged
          </span>
          <div className="mt-1 flex items-baseline gap-1">
            <span className="text-2xl font-extrabold text-emerald-600">
              {filteredEntries.length > 0
                ? (totalQuantity / filteredEntries.length).toFixed(1)
                : '0.0'}
            </span>
            <span className="text-xs font-semibold text-slate-500">L / fill</span>
          </div>
        </div>
      </div>

      {/* Logged Fuel List */}
      {filteredEntries.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm">
          <Fuel className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">No Fuel Entries Added Yet.</h3>
          <p className="text-xs text-slate-500 mt-1">
            Start tracking your fuel costs and mileage logs.
          </p>
          <button
            onClick={() => setIsFormOpen(true)}
            className="mt-4 px-4 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold shadow hover:bg-emerald-500 transition-all"
          >
            Add First Fuel Entry
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredEntries.map((fuel) => (
            <div
              key={fuel.id}
              className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow transition-all flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-extrabold text-slate-900">
                    {fuel.vehicleName}
                  </span>
                  <span className="bg-emerald-50 text-emerald-700 text-xs font-semibold px-2 py-0.5 rounded-full border border-emerald-200">
                    {fuel.fuelType}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-x-4 gap-y-1 text-xs text-slate-600 pt-1">
                  <div>
                    <span className="text-slate-400">Date:</span>{' '}
                    <span className="font-semibold text-slate-800">{fuel.fuelDate}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Quantity:</span>{' '}
                    <span className="font-semibold text-slate-800">
                      {fuel.fuelQuantity} Liter
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400">Amount Paid:</span>{' '}
                    <span className="font-bold text-emerald-600">${fuel.amountPaid}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Odometer:</span>{' '}
                    <span className="font-semibold text-slate-800">
                      {fuel.currentOdometer}
                    </span>
                  </div>
                </div>

                {fuel.fuelStationName && (
                  <p className="text-xs text-slate-500">
                    Station: <span className="font-medium">{fuel.fuelStationName}</span>
                  </p>
                )}
                {fuel.notes && (
                  <p className="text-xs text-slate-500 italic">Notes: {fuel.notes}</p>
                )}
              </div>

              <div className="self-end md:self-center">
                <button
                  onClick={() => {
                    if (window.confirm('Delete this fuel entry?')) {
                      onDeleteFuelEntry(fuel.id);
                    }
                  }}
                  className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all"
                  title="Delete Entry"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Fuel Form Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
              <h2 className="text-lg font-extrabold flex items-center gap-2">
                <Fuel className="w-5 h-5 text-emerald-400" />
                <span>Add Fuel Entry</span>
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
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
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
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                    />
                  )}
                </div>

                <div>
                  <label className="block mb-1">Fuel Date</label>
                  <input
                    type="date"
                    required
                    value={formData.fuelDate}
                    onChange={(e) =>
                      setFormData({ ...formData, fuelDate: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Fuel Type</label>
                  <input
                    type="text"
                    placeholder="e.g. Petrol, Diesel"
                    value={formData.fuelType}
                    onChange={(e) =>
                      setFormData({ ...formData, fuelType: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Fuel Quantity (Liters)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="e.g. 42.5"
                    value={formData.fuelQuantity}
                    onChange={(e) =>
                      setFormData({ ...formData, fuelQuantity: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Amount Paid ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="e.g. 63.75"
                    value={formData.amountPaid}
                    onChange={(e) =>
                      setFormData({ ...formData, amountPaid: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
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
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Fuel Station Name</label>
                  <input
                    type="text"
                    placeholder="e.g. Shell, Chevron, BP"
                    value={formData.fuelStationName}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        fuelStationName: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Notes</label>
                  <textarea
                    rows={2}
                    placeholder="Additional details..."
                    value={formData.notes}
                    onChange={(e) =>
                      setFormData({ ...formData, notes: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500"
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
                  className="flex items-center gap-1.5 px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow transition-all"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Save Fuel Entry</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

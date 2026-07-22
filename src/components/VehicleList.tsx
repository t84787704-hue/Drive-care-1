import React, { useState } from 'react';
import {
  Car,
  Plus,
  Search,
  Edit,
  Trash2,
  Info,
  ChevronRight,
  X,
  Gauge,
  CheckCircle2,
} from 'lucide-react';
import { Vehicle } from '../types';
import { VehicleDetailModal } from './VehicleDetailModal';

interface VehicleListProps {
  vehicles: Vehicle[];
  onSaveVehicle: (vehicle: Vehicle) => void;
  onDeleteVehicle: (id: number) => void;
  onNavigateTab: (
    tab: 'fuel' | 'maintenance' | 'documents',
    vehicleName: string
  ) => void;
  initialAddOpen?: boolean;
}

export const VehicleList: React.FC<VehicleListProps> = ({
  vehicles,
  onSaveVehicle,
  onDeleteVehicle,
  onNavigateTab,
  initialAddOpen = false,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(initialAddOpen);
  const [editingVehicle, setEditingVehicle] = useState<Vehicle | null>(null);

  // Form State
  const [formData, setFormData] = useState({
    vehicleName: '',
    vehicleType: 'Sedan',
    brand: '',
    model: '',
    manufacturingYear: new Date().getFullYear().toString(),
    registrationNumber: '',
    engineNumber: '',
    chassisNumber: '',
    fuelType: 'Petrol',
    odometerReading: '0',
    purchaseDate: new Date().toISOString().split('T')[0],
    insuranceDetails: '',
    country: 'USA',
    distanceUnit: 'km',
    notes: '',
  });

  const handleOpenAdd = () => {
    setEditingVehicle(null);
    setFormData({
      vehicleName: '',
      vehicleType: 'Sedan',
      brand: '',
      model: '',
      manufacturingYear: new Date().getFullYear().toString(),
      registrationNumber: '',
      engineNumber: '',
      chassisNumber: '',
      fuelType: 'Petrol',
      odometerReading: '0',
      purchaseDate: new Date().toISOString().split('T')[0],
      insuranceDetails: '',
      country: 'USA',
      distanceUnit: 'km',
      notes: '',
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = (v: Vehicle) => {
    setEditingVehicle(v);
    setFormData({
      vehicleName: v.vehicleName,
      vehicleType: v.vehicleType,
      brand: v.brand,
      model: v.model,
      manufacturingYear: v.manufacturingYear,
      registrationNumber: v.registrationNumber,
      engineNumber: v.engineNumber,
      chassisNumber: v.chassisNumber,
      fuelType: v.fuelType,
      odometerReading: v.odometerReading,
      purchaseDate: v.purchaseDate,
      insuranceDetails: v.insuranceDetails,
      country: v.country,
      distanceUnit: v.distanceUnit || 'km',
      notes: v.notes,
    });
    setIsFormOpen(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (
      !formData.vehicleName.trim() ||
      !formData.brand.trim() ||
      !formData.model.trim()
    ) {
      alert('Please fill in required fields: Vehicle Name, Brand, and Model.');
      return;
    }

    const vehicleToSave: Vehicle = {
      id: editingVehicle ? editingVehicle.id : 0,
      vehicleName: formData.vehicleName.trim(),
      vehicleType: formData.vehicleType,
      brand: formData.brand.trim(),
      model: formData.model.trim(),
      manufacturingYear: formData.manufacturingYear,
      registrationNumber: formData.registrationNumber.trim(),
      engineNumber: formData.engineNumber.trim(),
      chassisNumber: formData.chassisNumber.trim(),
      fuelType: formData.fuelType,
      odometerReading: formData.odometerReading.trim(),
      purchaseDate: formData.purchaseDate,
      insuranceDetails: formData.insuranceDetails.trim(),
      vehiclePhoto: '',
      country: formData.country.trim(),
      distanceUnit: formData.distanceUnit,
      notes: formData.notes.trim(),
      createdAt: editingVehicle ? editingVehicle.createdAt : Date.now(),
      updatedAt: Date.now(),
    };

    onSaveVehicle(vehicleToSave);
    setIsFormOpen(false);
    setEditingVehicle(null);
  };

  const filteredVehicles = vehicles.filter(
    (v) =>
      v.vehicleName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.brand.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.model.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.registrationNumber.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Car className="w-6 h-6 text-blue-600" />
            <span>My Vehicles ({vehicles.length})</span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Total registered vehicles in DriveCare database
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          {/* Search bar */}
          <div className="relative flex-1 sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by name, brand, reg..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
            />
          </div>

          <button
            onClick={handleOpenAdd}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold shadow transition-all whitespace-nowrap"
          >
            <Plus className="w-4 h-4" />
            <span>Add Vehicle</span>
          </button>
        </div>
      </div>

      {/* Vehicle Grid List */}
      {filteredVehicles.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm">
          <Car className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">No Vehicles Found</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
            {searchTerm
              ? 'No vehicle matches your filter search term.'
              : 'You have not added any vehicles yet. Click below to register your vehicle.'}
          </p>
          {!searchTerm && (
            <button
              onClick={handleOpenAdd}
              className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-xl text-xs font-bold shadow hover:bg-blue-500 transition-all"
            >
              Add Vehicle
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredVehicles.map((vehicle) => (
            <div
              key={vehicle.id}
              className="bg-white rounded-2xl border border-slate-200/90 shadow-sm hover:shadow-md transition-all overflow-hidden flex flex-col justify-between group"
            >
              {/* Card Header */}
              <div className="p-5 border-b border-slate-100 bg-gradient-to-r from-slate-900 to-slate-800 text-white">
                <div className="flex items-start justify-between">
                  <div>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-blue-400 bg-blue-500/20 px-2 py-0.5 rounded-full border border-blue-400/30">
                      {vehicle.vehicleType}
                    </span>
                    <h3 className="text-lg font-extrabold mt-1 text-white">
                      {vehicle.vehicleName}
                    </h3>
                    <p className="text-xs text-slate-300">
                      {vehicle.brand} {vehicle.model} ({vehicle.manufacturingYear})
                    </p>
                  </div>

                  <div className="text-right">
                    <span className="text-xs font-extrabold text-blue-300 block">
                      {vehicle.odometerReading} {vehicle.distanceUnit || 'km'}
                    </span>
                    <span className="text-[10px] text-slate-400 block mt-0.5">
                      {vehicle.fuelType}
                    </span>
                  </div>
                </div>
              </div>

              {/* Card Specs */}
              <div className="p-5 space-y-2.5 text-xs">
                <div className="flex justify-between text-slate-600">
                  <span className="text-slate-400 font-medium">Registration:</span>
                  <span className="font-bold text-slate-800">
                    {vehicle.registrationNumber || 'N/A'}
                  </span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span className="text-slate-400 font-medium">Engine Number:</span>
                  <span className="font-semibold text-slate-700">
                    {vehicle.engineNumber || 'N/A'}
                  </span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span className="text-slate-400 font-medium">Insurance:</span>
                  <span className="font-semibold text-slate-700 truncate max-w-[180px]">
                    {vehicle.insuranceDetails || 'N/A'}
                  </span>
                </div>
              </div>

              {/* Card Actions Footer */}
              <div className="px-5 py-3.5 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
                <button
                  onClick={() => setSelectedVehicle(vehicle)}
                  className="text-xs font-bold text-blue-600 hover:text-blue-700 flex items-center gap-1 group-hover:translate-x-0.5 transition-transform"
                >
                  <Info className="w-3.5 h-3.5" />
                  <span>View Details</span>
                </button>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => handleOpenEdit(vehicle)}
                    title="Edit Vehicle"
                    className="p-1.5 rounded-lg text-slate-500 hover:text-blue-600 hover:bg-blue-50 transition-all"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => {
                      if (
                        window.confirm(
                          `Delete ${vehicle.vehicleName}?`
                        )
                      ) {
                        onDeleteVehicle(vehicle.id);
                      }
                    }}
                    title="Delete Vehicle"
                    className="p-1.5 rounded-lg text-slate-500 hover:text-red-600 hover:bg-red-50 transition-all"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Detail Modal */}
      <VehicleDetailModal
        vehicle={selectedVehicle}
        onClose={() => setSelectedVehicle(null)}
        onEdit={(v) => handleOpenEdit(v)}
        onDelete={(id) => onDeleteVehicle(id)}
        onNavigateTab={onNavigateTab}
      />

      {/* Add / Edit Form Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-xl w-full shadow-2xl border border-slate-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
              <h2 className="text-lg font-extrabold flex items-center gap-2">
                <Car className="w-5 h-5 text-blue-400" />
                <span>{editingVehicle ? 'Edit Vehicle' : 'Add New Vehicle'}</span>
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
                  <input
                    type="text"
                    required
                    placeholder="e.g. Toyota Camry Hybrid"
                    value={formData.vehicleName}
                    onChange={(e) =>
                      setFormData({ ...formData, vehicleName: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Vehicle Type</label>
                  <select
                    value={formData.vehicleType}
                    onChange={(e) =>
                      setFormData({ ...formData, vehicleType: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  >
                    <option value="Sedan">Sedan</option>
                    <option value="SUV">SUV</option>
                    <option value="Hatchback">Hatchback</option>
                    <option value="Truck">Truck</option>
                    <option value="Motorcycle">Motorcycle</option>
                    <option value="Van">Van / MPV</option>
                    <option value="Coupe">Coupe / Convertible</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1">Brand / Make *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Toyota, Honda, Ford"
                    value={formData.brand}
                    onChange={(e) =>
                      setFormData({ ...formData, brand: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Model *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Camry, Civic, F-150"
                    value={formData.model}
                    onChange={(e) =>
                      setFormData({ ...formData, model: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Manufacturing Year</label>
                  <input
                    type="number"
                    placeholder="e.g. 2022"
                    value={formData.manufacturingYear}
                    onChange={(e) =>
                      setFormData({ ...formData, manufacturingYear: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Registration Number</label>
                  <input
                    type="text"
                    placeholder="e.g. ABC-1234"
                    value={formData.registrationNumber}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        registrationNumber: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Fuel Type</label>
                  <select
                    value={formData.fuelType}
                    onChange={(e) =>
                      setFormData({ ...formData, fuelType: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  >
                    <option value="Petrol">Petrol / Gasoline</option>
                    <option value="Diesel">Diesel</option>
                    <option value="Hybrid">Hybrid</option>
                    <option value="Electric">Electric (EV)</option>
                    <option value="CNG">CNG / LPG</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1">Odometer Reading</label>
                  <input
                    type="number"
                    placeholder="e.g. 28450"
                    value={formData.odometerReading}
                    onChange={(e) =>
                      setFormData({ ...formData, odometerReading: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Distance Unit</label>
                  <select
                    value={formData.distanceUnit}
                    onChange={(e) =>
                      setFormData({ ...formData, distanceUnit: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  >
                    <option value="km">Kilometers (km)</option>
                    <option value="miles">Miles (mi)</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1">Engine Number</label>
                  <input
                    type="text"
                    placeholder="e.g. 2AR-FXE-991"
                    value={formData.engineNumber}
                    onChange={(e) =>
                      setFormData({ ...formData, engineNumber: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Chassis Number / VIN</label>
                  <input
                    type="text"
                    placeholder="e.g. JTEBU22F8820"
                    value={formData.chassisNumber}
                    onChange={(e) =>
                      setFormData({ ...formData, chassisNumber: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Insurance Details</label>
                  <input
                    type="text"
                    placeholder="e.g. State Farm #POL-889102"
                    value={formData.insuranceDetails}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        insuranceDetails: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Notes</label>
                  <textarea
                    rows={2}
                    placeholder="Additional notes about this vehicle..."
                    value={formData.notes}
                    onChange={(e) =>
                      setFormData({ ...formData, notes: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
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
                  className="flex items-center gap-1.5 px-5 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold shadow transition-all"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>{editingVehicle ? 'Update Vehicle' : 'Save Vehicle'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

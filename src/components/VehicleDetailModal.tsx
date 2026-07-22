import React from 'react';
import {
  X,
  Edit,
  Trash2,
  Wrench,
  Fuel,
  FileText,
  Shield,
  Car,
  Calendar,
  Gauge,
  Globe,
} from 'lucide-react';
import { Vehicle } from '../types';

interface VehicleDetailModalProps {
  vehicle: Vehicle | null;
  onClose: () => void;
  onEdit: (vehicle: Vehicle) => void;
  onDelete: (id: number) => void;
  onNavigateTab: (
    tab: 'fuel' | 'maintenance' | 'documents',
    vehicleName: string
  ) => void;
}

export const VehicleDetailModal: React.FC<VehicleDetailModalProps> = ({
  vehicle,
  onClose,
  onEdit,
  onDelete,
  onNavigateTab,
}) => {
  if (!vehicle) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-2xl w-full shadow-2xl border border-slate-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-200">
        {/* Modal Header */}
        <div className="bg-slate-900 text-white p-6 relative">
          <button
            onClick={onClose}
            className="absolute top-5 right-5 p-2 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
          >
            <X className="w-5 h-5" />
          </button>
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-blue-600 flex items-center justify-center text-white font-bold shadow-md">
              <Car className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-2xl font-extrabold tracking-tight">
                {vehicle.vehicleName}
              </h2>
              <p className="text-blue-300 text-xs font-semibold">
                {vehicle.brand} {vehicle.model} ({vehicle.manufacturingYear})
              </p>
            </div>
          </div>
        </div>

        {/* Modal Body */}
        <div className="p-6 space-y-6 max-h-[70vh] overflow-y-auto">
          {/* Top Quick Badges */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] font-bold uppercase text-slate-400 block">
                Type
              </span>
              <span className="text-sm font-extrabold text-slate-800">
                {vehicle.vehicleType || 'N/A'}
              </span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] font-bold uppercase text-slate-400 block">
                Fuel Type
              </span>
              <span className="text-sm font-extrabold text-slate-800">
                {vehicle.fuelType || 'N/A'}
              </span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] font-bold uppercase text-slate-400 block">
                Odometer
              </span>
              <span className="text-sm font-extrabold text-blue-600">
                {vehicle.odometerReading} {vehicle.distanceUnit || 'km'}
              </span>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80">
              <span className="text-[10px] font-bold uppercase text-slate-400 block">
                Registration
              </span>
              <span className="text-sm font-extrabold text-slate-800">
                {vehicle.registrationNumber || 'N/A'}
              </span>
            </div>
          </div>

          {/* Detailed Info Grid */}
          <div className="bg-slate-50/70 p-4 rounded-xl border border-slate-200 space-y-3">
            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Technical Specifications & Identification
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-xs">
              <div>
                <span className="text-slate-500 font-medium">Engine Number:</span>{' '}
                <span className="font-semibold text-slate-800">
                  {vehicle.engineNumber || 'Not specified'}
                </span>
              </div>
              <div>
                <span className="text-slate-500 font-medium">Chassis / VIN:</span>{' '}
                <span className="font-semibold text-slate-800">
                  {vehicle.chassisNumber || 'Not specified'}
                </span>
              </div>
              <div>
                <span className="text-slate-500 font-medium">Purchase Date:</span>{' '}
                <span className="font-semibold text-slate-800">
                  {vehicle.purchaseDate || 'Not specified'}
                </span>
              </div>
              <div>
                <span className="text-slate-500 font-medium">Country / Region:</span>{' '}
                <span className="font-semibold text-slate-800">
                  {vehicle.country || 'Not specified'}
                </span>
              </div>
              <div className="sm:col-span-2">
                <span className="text-slate-500 font-medium">Insurance Details:</span>{' '}
                <span className="font-semibold text-slate-800">
                  {vehicle.insuranceDetails || 'No insurance record'}
                </span>
              </div>
              {vehicle.notes && (
                <div className="sm:col-span-2 border-t border-slate-200/60 pt-2 mt-1">
                  <span className="text-slate-500 font-medium block">Notes:</span>
                  <p className="text-slate-700 italic">{vehicle.notes}</p>
                </div>
              )}
            </div>
          </div>

          {/* Quick Shortcuts to Related Modules */}
          <div>
            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Vehicle History Shortcuts
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              <button
                onClick={() => {
                  onClose();
                  onNavigateTab('maintenance', vehicle.vehicleName);
                }}
                className="flex items-center gap-2 p-3 bg-purple-50 hover:bg-purple-100/80 text-purple-800 rounded-xl text-xs font-bold transition-all border border-purple-200/80"
              >
                <Wrench className="w-4 h-4 text-purple-600" />
                <span>Service History</span>
              </button>

              <button
                onClick={() => {
                  onClose();
                  onNavigateTab('fuel', vehicle.vehicleName);
                }}
                className="flex items-center gap-2 p-3 bg-emerald-50 hover:bg-emerald-100/80 text-emerald-800 rounded-xl text-xs font-bold transition-all border border-emerald-200/80"
              >
                <Fuel className="w-4 h-4 text-emerald-600" />
                <span>Fuel History</span>
              </button>

              <button
                onClick={() => {
                  onClose();
                  onNavigateTab('documents', vehicle.vehicleName);
                }}
                className="flex items-center gap-2 p-3 bg-indigo-50 hover:bg-indigo-100/80 text-indigo-800 rounded-xl text-xs font-bold transition-all border border-indigo-200/80"
              >
                <FileText className="w-4 h-4 text-indigo-600" />
                <span>Vehicle Documents</span>
              </button>
            </div>
          </div>
        </div>

        {/* Modal Footer Actions */}
        <div className="bg-slate-50 p-4 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3">
          <button
            onClick={() => {
              if (
                window.confirm(
                  `Are you sure you want to delete ${vehicle.vehicleName}?`
                )
              ) {
                onDelete(vehicle.id);
                onClose();
              }
            }}
            className="flex items-center gap-1.5 px-3 py-2 bg-red-50 hover:bg-red-100 text-red-700 rounded-xl text-xs font-bold border border-red-200 transition-all"
          >
            <Trash2 className="w-4 h-4" />
            <span>Delete Vehicle</span>
          </button>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-xl text-xs font-bold transition-all"
            >
              Close
            </button>
            <button
              onClick={() => {
                onClose();
                onEdit(vehicle);
              }}
              className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold shadow transition-all"
            >
              <Edit className="w-4 h-4" />
              <span>Edit Vehicle</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

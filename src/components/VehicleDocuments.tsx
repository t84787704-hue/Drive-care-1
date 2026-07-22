import React, { useState } from 'react';
import {
  FileText,
  Plus,
  Trash2,
  X,
  CheckCircle2,
  Filter,
  ShieldCheck,
  AlertTriangle,
  Calendar,
} from 'lucide-react';
import { Document, Vehicle } from '../types';

interface VehicleDocumentsProps {
  documents: Document[];
  vehicles: Vehicle[];
  onSaveDocument: (doc: Document) => void;
  onDeleteDocument: (id: number) => void;
  initialVehicleFilter?: string;
  initialAddOpen?: boolean;
}

export const VehicleDocuments: React.FC<VehicleDocumentsProps> = ({
  documents,
  vehicles,
  onSaveDocument,
  onDeleteDocument,
  initialVehicleFilter = '',
  initialAddOpen = false,
}) => {
  const [selectedVehicleFilter, setSelectedVehicleFilter] =
    useState(initialVehicleFilter);
  const [isFormOpen, setIsFormOpen] = useState(initialAddOpen);

  const [formData, setFormData] = useState({
    vehicleName: vehicles[0]?.vehicleName || '',
    documentTitle: '',
    documentType: 'Insurance Policy',
    documentNumber: '',
    issueDate: new Date().toISOString().split('T')[0],
    expiryDate: new Date(Date.now() + 365 * 24 * 3600 * 1000)
      .toISOString()
      .split('T')[0],
    notes: '',
  });

  const filteredDocuments = selectedVehicleFilter
    ? documents.filter(
        (d) =>
          d.vehicleName.toLowerCase() === selectedVehicleFilter.toLowerCase()
      )
    : documents;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.vehicleName.trim() || !formData.documentTitle.trim()) {
      alert('Please fill in required fields: Vehicle Name and Document Title.');
      return;
    }

    const newDoc: Document = {
      id: Date.now(),
      vehicleName: formData.vehicleName.trim(),
      documentTitle: formData.documentTitle.trim(),
      documentType: formData.documentType,
      documentNumber: formData.documentNumber.trim(),
      issueDate: formData.issueDate,
      expiryDate: formData.expiryDate,
      notes: formData.notes.trim(),
      createdAt: Date.now(),
    };

    onSaveDocument(newDoc);
    setIsFormOpen(false);
  };

  const getDocStatus = (expiryDate: string) => {
    if (!expiryDate) return { text: 'Active', color: 'bg-emerald-100 text-emerald-800' };
    const diff = new Date(expiryDate).getTime() - new Date().getTime();
    const days = Math.ceil(diff / (1000 * 3600 * 24));
    if (days < 0) return { text: 'Expired', color: 'bg-red-100 text-red-800' };
    if (days <= 30) return { text: `Expires in ${days}d`, color: 'bg-amber-100 text-amber-800' };
    return { text: 'Valid & Active', color: 'bg-emerald-100 text-emerald-800' };
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <FileText className="w-6 h-6 text-indigo-600" />
            <span>
              {selectedVehicleFilter
                ? `${selectedVehicleFilter} - Documents`
                : 'Vehicle Documents'}
            </span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Total Documents: {filteredDocuments.length}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
          {/* Filter */}
          <div className="flex items-center gap-1.5 bg-slate-50 px-3 py-1.5 border border-slate-200 rounded-xl text-xs">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <select
              value={selectedVehicleFilter}
              onChange={(e) => setSelectedVehicleFilter(e.target.value)}
              className="bg-transparent text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
            >
              <option value="">All Vehicles ({documents.length})</option>
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
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold shadow transition-all whitespace-nowrap"
          >
            <Plus className="w-4 h-4" />
            <span>Add Document</span>
          </button>
        </div>
      </div>

      {/* Documents List */}
      {filteredDocuments.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm">
          <FileText className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">
            No Documents Added Yet.
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Store digital records of vehicle registration, insurance policy cards, driving licenses, and warranty documents.
          </p>
          <button
            onClick={() => setIsFormOpen(true)}
            className="mt-4 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-bold shadow hover:bg-indigo-500 transition-all"
          >
            Add First Document
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredDocuments.map((doc) => {
            const status = getDocStatus(doc.expiryDate);

            return (
              <div
                key={doc.id}
                className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                        {doc.documentType}
                      </span>
                      <h3 className="text-base font-extrabold text-slate-900 mt-1">
                        {doc.documentTitle}
                      </h3>
                      <p className="text-xs font-semibold text-slate-600 mt-0.5">
                        {doc.vehicleName}
                      </p>
                    </div>

                    <span
                      className={`text-xs font-bold px-2.5 py-1 rounded-lg border border-transparent ${status.color}`}
                    >
                      {status.text}
                    </span>
                  </div>

                  <div className="mt-4 pt-3 border-t border-slate-100 space-y-1 text-xs text-slate-600">
                    <div>
                      <span className="text-slate-400 font-medium">Doc Number:</span>{' '}
                      <span className="font-bold text-slate-800">
                        {doc.documentNumber || 'N/A'}
                      </span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 pt-1">
                      <div>
                        <span className="text-slate-400 block font-medium">Issue Date:</span>
                        <span className="font-semibold text-slate-700">{doc.issueDate}</span>
                      </div>
                      <div>
                        <span className="text-slate-400 block font-medium">Expiry Date:</span>
                        <span className="font-semibold text-slate-700">{doc.expiryDate}</span>
                      </div>
                    </div>
                  </div>

                  {doc.notes && (
                    <p className="text-xs text-slate-500 italic mt-3 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                      Notes: {doc.notes}
                    </p>
                  )}
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-end">
                  <button
                    onClick={() => {
                      if (window.confirm('Delete this document entry?')) {
                        onDeleteDocument(doc.id);
                      }
                    }}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-xl text-xs font-bold transition-all"
                  >
                    <Trash2 className="w-4 h-4" />
                    <span>Delete Record</span>
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
                <FileText className="w-5 h-5 text-indigo-400" />
                <span>Add Vehicle Document</span>
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
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
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
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                    />
                  )}
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Document Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Comprehensive Auto Insurance Policy"
                    value={formData.documentTitle}
                    onChange={(e) =>
                      setFormData({ ...formData, documentTitle: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Document Type</label>
                  <select
                    value={formData.documentType}
                    onChange={(e) =>
                      setFormData({ ...formData, documentType: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  >
                    <option value="Insurance Policy">Insurance Policy</option>
                    <option value="Registration / RC">Registration / RC</option>
                    <option value="Driving License">Driving License</option>
                    <option value="Pollution / PUC">Pollution / Emission (PUC)</option>
                    <option value="Warranty / Service Card">Warranty Card</option>
                    <option value="Other">Other Document</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1">Document Number</label>
                  <input
                    type="text"
                    placeholder="e.g. POL-889102-SF"
                    value={formData.documentNumber}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        documentNumber: e.target.value,
                      })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Issue Date</label>
                  <input
                    type="date"
                    value={formData.issueDate}
                    onChange={(e) =>
                      setFormData({ ...formData, issueDate: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Expiry Date</label>
                  <input
                    type="date"
                    value={formData.expiryDate}
                    onChange={(e) =>
                      setFormData({ ...formData, expiryDate: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Notes</label>
                  <textarea
                    rows={2}
                    placeholder="Additional information about policy or location..."
                    value={formData.notes}
                    onChange={(e) =>
                      setFormData({ ...formData, notes: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
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
                  className="flex items-center gap-1.5 px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold shadow transition-all"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Save Document</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

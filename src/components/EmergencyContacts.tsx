import React, { useState } from 'react';
import {
  PhoneCall,
  Plus,
  Trash2,
  X,
  CheckCircle2,
  Copy,
  Phone,
  ShieldAlert,
  MapPin,
  Check,
} from 'lucide-react';
import { EmergencyContact } from '../types';

interface EmergencyContactsProps {
  contacts: EmergencyContact[];
  onSaveContact: (contact: EmergencyContact) => void;
  onDeleteContact: (id: number) => void;
  initialAddOpen?: boolean;
}

export const EmergencyContacts: React.FC<EmergencyContactsProps> = ({
  contacts,
  onSaveContact,
  onDeleteContact,
  initialAddOpen = false,
}) => {
  const [isFormOpen, setIsFormOpen] = useState(initialAddOpen);
  const [copiedId, setCopiedId] = useState<number | null>(null);

  const [formData, setFormData] = useState({
    contactName: '',
    contactCategory: 'Roadside Assistance',
    phoneNumber: '',
    locationRegion: 'National Hotline',
    notes: '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.contactName.trim() || !formData.phoneNumber.trim()) {
      alert('Please fill in Name and Phone Number.');
      return;
    }

    const newContact: EmergencyContact = {
      id: Date.now(),
      contactName: formData.contactName.trim(),
      contactCategory: formData.contactCategory,
      phoneNumber: formData.phoneNumber.trim(),
      locationRegion: formData.locationRegion.trim(),
      notes: formData.notes.trim(),
      createdAt: Date.now(),
    };

    onSaveContact(newContact);
    setIsFormOpen(false);
    setFormData({
      contactName: '',
      contactCategory: 'Roadside Assistance',
      phoneNumber: '',
      locationRegion: 'National Hotline',
      notes: '',
    });
  };

  const copyToClipboard = (id: number, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Emergency Banner */}
      <div className="bg-gradient-to-r from-red-600 via-rose-600 to-red-700 text-white rounded-2xl p-6 shadow-md flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <ShieldAlert className="w-6 h-6 animate-pulse" />
            <h1 className="text-xl sm:text-2xl font-extrabold tracking-tight">
              Emergency & Roadside Contacts
            </h1>
          </div>
          <p className="text-red-100 text-xs mt-1 max-w-lg">
            Instant access to 24/7 towing services, vehicle insurance hotlines, police, and personal contacts during roadside emergencies.
          </p>
        </div>

        <button
          onClick={() => setIsFormOpen(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-white hover:bg-slate-100 text-red-700 rounded-xl text-xs font-extrabold shadow transition-all whitespace-nowrap"
        >
          <Plus className="w-4 h-4" />
          <span>Add Emergency Contact</span>
        </button>
      </div>

      {/* Contacts List Grid */}
      {contacts.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm">
          <PhoneCall className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">No Emergency Contacts Stored.</h3>
          <p className="text-xs text-slate-500 mt-1">
            Add essential emergency contacts like AAA towing, roadside assist, or family numbers.
          </p>
          <button
            onClick={() => setIsFormOpen(true)}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-xl text-xs font-bold shadow hover:bg-red-500 transition-all"
          >
            Add First Contact
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {contacts.map((contact) => (
            <div
              key={contact.id}
              className="bg-white rounded-2xl p-5 border border-slate-200/90 shadow-sm hover:shadow-md transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between">
                  <span className="text-[10px] font-extrabold uppercase tracking-wider text-red-700 bg-red-50 px-2.5 py-0.5 rounded-full border border-red-100">
                    {contact.contactCategory}
                  </span>
                  <button
                    onClick={() => {
                      if (window.confirm(`Delete contact ${contact.contactName}?`)) {
                        onDeleteContact(contact.id);
                      }
                    }}
                    className="p-1 text-slate-400 hover:text-red-600 rounded"
                    title="Delete Contact"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>

                <h3 className="text-lg font-extrabold text-slate-900 mt-2">
                  {contact.contactName}
                </h3>

                <p className="text-xs font-bold text-slate-700 mt-1 flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-slate-400" />
                  <span>{contact.locationRegion || 'Global'}</span>
                </p>

                {contact.notes && (
                  <p className="text-xs text-slate-500 italic mt-2 bg-slate-50 p-2 rounded-lg border border-slate-100">
                    {contact.notes}
                  </p>
                )}
              </div>

              <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                <a
                  href={`tel:${contact.phoneNumber}`}
                  className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-bold shadow transition-all"
                >
                  <Phone className="w-3.5 h-3.5" />
                  <span>Call {contact.phoneNumber}</span>
                </a>

                <button
                  onClick={() => copyToClipboard(contact.id, contact.phoneNumber)}
                  className="p-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl transition-all"
                  title="Copy Phone Number"
                >
                  {copiedId === contact.id ? (
                    <Check className="w-4 h-4 text-emerald-600" />
                  ) : (
                    <Copy className="w-4 h-4" />
                  )}
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
                <ShieldAlert className="w-5 h-5 text-red-400" />
                <span>Add Emergency Contact</span>
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
                  <label className="block mb-1">Contact / Service Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. AAA 24/7 Roadside Assistance"
                    value={formData.contactName}
                    onChange={(e) =>
                      setFormData({ ...formData, contactName: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Category</label>
                  <select
                    value={formData.contactCategory}
                    onChange={(e) =>
                      setFormData({ ...formData, contactCategory: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500"
                  >
                    <option value="Roadside Assistance">Roadside Assistance</option>
                    <option value="Towing Service">Towing Service</option>
                    <option value="Insurance Claim Hotline">Insurance Hotline</option>
                    <option value="Local Police / Highway Patrol">Police / Highway Patrol</option>
                    <option value="Ambulance / Hospital">Medical / Ambulance</option>
                    <option value="Personal / Family">Family Contact</option>
                  </select>
                </div>

                <div>
                  <label className="block mb-1">Phone Number *</label>
                  <input
                    type="tel"
                    required
                    placeholder="e.g. +1-800-222-4357"
                    value={formData.phoneNumber}
                    onChange={(e) =>
                      setFormData({ ...formData, phoneNumber: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Coverage Area / Region</label>
                  <input
                    type="text"
                    placeholder="e.g. Statewide / Nationwide"
                    value={formData.locationRegion}
                    onChange={(e) =>
                      setFormData({ ...formData, locationRegion: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block mb-1">Notes</label>
                  <textarea
                    rows={2}
                    placeholder="e.g. Policy #123456 required when calling..."
                    value={formData.notes}
                    onChange={(e) =>
                      setFormData({ ...formData, notes: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500"
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
                  className="flex items-center gap-1.5 px-5 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-bold shadow transition-all"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Save Contact</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

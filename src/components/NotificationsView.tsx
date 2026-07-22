import React, { useState } from 'react';
import {
  Bell,
  Plus,
  Trash2,
  X,
  CheckCircle2,
  CheckCheck,
  AlertCircle,
  Calendar,
} from 'lucide-react';
import { NotificationItem } from '../types';

interface NotificationsViewProps {
  notifications: NotificationItem[];
  onSaveNotification: (notif: NotificationItem) => void;
  onDeleteNotification: (id: number) => void;
  onMarkAllRead: () => void;
}

export const NotificationsView: React.FC<NotificationsViewProps> = ({
  notifications,
  onSaveNotification,
  onDeleteNotification,
  onMarkAllRead,
}) => {
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    message: '',
    vehicleName: '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title.trim() || !formData.message.trim()) {
      alert('Please fill in Title and Message.');
      return;
    }

    const newNotif: NotificationItem = {
      id: Date.now(),
      title: formData.title.trim(),
      message: formData.message.trim(),
      vehicleName: formData.vehicleName.trim(),
      timestamp: Date.now(),
      isRead: false,
    };

    onSaveNotification(newNotif);
    setIsFormOpen(false);
    setFormData({ title: '', message: '', vehicleName: '' });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Bell className="w-6 h-6 text-blue-600" />
            <span>Notification Feed ({notifications.length})</span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            System announcements, service alerts, and log events
          </p>
        </div>

        <div className="flex items-center gap-2">
          {notifications.length > 0 && (
            <button
              onClick={onMarkAllRead}
              className="flex items-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-all"
            >
              <CheckCheck className="w-4 h-4 text-blue-600" />
              <span>Mark All Read</span>
            </button>
          )}

          <button
            onClick={() => setIsFormOpen(true)}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold shadow transition-all whitespace-nowrap"
          >
            <Plus className="w-4 h-4" />
            <span>Add Alert</span>
          </button>
        </div>
      </div>

      {/* Notifications List */}
      {notifications.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm">
          <Bell className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">No Notifications</h3>
          <p className="text-xs text-slate-500 mt-1">
            You are all caught up! Service alerts and maintenance reminders will show up here.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((notif) => (
            <div
              key={notif.id}
              className={`p-4 rounded-2xl border transition-all flex items-start justify-between gap-4 ${
                notif.isRead
                  ? 'bg-white border-slate-200/80'
                  : 'bg-blue-50/50 border-blue-200'
              }`}
            >
              <div className="flex items-start gap-3">
                <div
                  className={`p-2.5 rounded-xl ${
                    notif.isRead
                      ? 'bg-slate-100 text-slate-500'
                      : 'bg-blue-600 text-white'
                  }`}
                >
                  <Bell className="w-4 h-4" />
                </div>

                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-extrabold text-slate-900">
                      {notif.title}
                    </h3>
                    {notif.vehicleName && (
                      <span className="text-[10px] font-bold bg-slate-200/70 text-slate-700 px-2 py-0.5 rounded">
                        {notif.vehicleName}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-600 mt-1">{notif.message}</p>
                  <p className="text-[10px] text-slate-400 mt-2 font-medium">
                    {new Date(notif.timestamp).toLocaleString()}
                  </p>
                </div>
              </div>

              <button
                onClick={() => onDeleteNotification(notif.id)}
                className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all"
                title="Delete Notification"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Add Notification Form Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-200 overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
              <h2 className="text-lg font-extrabold flex items-center gap-2">
                <Bell className="w-5 h-5 text-blue-400" />
                <span>Create Custom Alert</span>
              </h2>
              <button
                onClick={() => setIsFormOpen(false)}
                className="p-1.5 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
              <div className="space-y-3 text-xs font-semibold text-slate-700">
                <div>
                  <label className="block mb-1">Alert Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Schedule Annual Vehicle Inspection"
                    value={formData.title}
                    onChange={(e) =>
                      setFormData({ ...formData, title: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Vehicle Tag (Optional)</label>
                  <input
                    type="text"
                    placeholder="e.g. Toyota Camry"
                    value={formData.vehicleName}
                    onChange={(e) =>
                      setFormData({ ...formData, vehicleName: e.target.value })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block mb-1">Message Content *</label>
                  <textarea
                    rows={3}
                    required
                    placeholder="Describe the reminder or alert details..."
                    value={formData.message}
                    onChange={(e) =>
                      setFormData({ ...formData, message: e.target.value })
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
                  <span>Send Alert</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

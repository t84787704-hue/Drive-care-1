import React, { useState, useRef, useEffect } from 'react';
import {
  Car,
  Fuel,
  Wrench,
  BellRing,
  FileText,
  PhoneCall,
  Bell,
  Settings,
  LayoutDashboard,
  ShieldAlert,
  Globe,
  ChevronDown,
} from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';
import { LANGUAGES, Language } from '../utils/i18n';

export type NavTab =
  | 'dashboard'
  | 'vehicles'
  | 'fuel'
  | 'maintenance'
  | 'reminders'
  | 'documents'
  | 'emergency'
  | 'notifications'
  | 'settings';

interface NavbarProps {
  activeTab: NavTab;
  setActiveTab: (tab: NavTab) => void;
  pendingRemindersCount: number;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  pendingRemindersCount,
}) => {
  const { t, language, setLanguage, currentLanguageObj } = useLanguage();
  const [langDropdownOpen, setLangDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setLangDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const navItems = [
    { id: 'dashboard' as NavTab, label: t('dashboard'), icon: LayoutDashboard },
    { id: 'vehicles' as NavTab, label: t('myVehicles'), icon: Car },
    { id: 'fuel' as NavTab, label: t('fuelTracker'), icon: Fuel },
    { id: 'maintenance' as NavTab, label: t('maintenance'), icon: Wrench },
    {
      id: 'reminders' as NavTab,
      label: t('reminders'),
      icon: BellRing,
      badge: pendingRemindersCount > 0 ? pendingRemindersCount : undefined,
    },
    { id: 'documents' as NavTab, label: t('documents'), icon: FileText },
    { id: 'emergency' as NavTab, label: t('emergency'), icon: PhoneCall },
    { id: 'notifications' as NavTab, label: t('notifications'), icon: Bell },
    { id: 'settings' as NavTab, label: t('settings'), icon: Settings },
  ];

  return (
    <header className="bg-slate-900 text-white shadow-lg sticky top-0 z-40 border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-2">
          {/* Logo & Title */}
          <div
            className="flex items-center gap-3 cursor-pointer group"
            onClick={() => setActiveTab('dashboard')}
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center text-white shadow-md group-hover:scale-105 transition-transform">
              <Car className="w-6 h-6" />
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight text-white flex items-center gap-1.5">
                {t('appName')}
                <span className="text-xs px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-400 font-medium border border-blue-500/30">
                  v1.0
                </span>
              </span>
              <p className="text-xs text-slate-400 font-medium hidden sm:block">
                {t('appSubtitle')}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {/* Language Dropdown Selector */}
            <div className="relative" ref={dropdownRef}>
              <button
                onClick={() => setLangDropdownOpen(!langDropdownOpen)}
                className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-all"
                title={t('selectLanguage')}
              >
                <Globe className="w-3.5 h-3.5 text-blue-400" />
                <span className="text-sm">{currentLanguageObj.flag}</span>
                <span className="hidden md:inline font-medium">{currentLanguageObj.nativeName}</span>
                <ChevronDown className="w-3 h-3 text-slate-400" />
              </button>

              {langDropdownOpen && (
                <div className="absolute right-0 rtl:right-auto rtl:left-0 mt-2 w-48 bg-slate-800 border border-slate-700 rounded-xl shadow-xl py-1 z-50 animate-in fade-in zoom-in-95 duration-100">
                  <div className="px-3 py-1.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-700/60">
                    {t('selectLanguage')}
                  </div>
                  {LANGUAGES.map((lang) => (
                    <button
                      key={lang.code}
                      onClick={() => {
                        setLanguage(lang.code as Language);
                        setLangDropdownOpen(false);
                      }}
                      className={`w-full flex items-center justify-between px-3 py-2 text-xs text-left rtl:text-right hover:bg-slate-700/70 transition-colors ${
                        language === lang.code
                          ? 'text-blue-400 font-bold bg-slate-700/40'
                          : 'text-slate-200'
                      }`}
                    >
                      <span className="flex items-center gap-2">
                        <span className="text-base">{lang.flag}</span>
                        <span>{lang.nativeName}</span>
                      </span>
                      <span className="text-[10px] text-slate-400 uppercase">
                        ({lang.code})
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Quick Emergency Action */}
            <button
              onClick={() => setActiveTab('emergency')}
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs sm:text-sm font-semibold rounded-lg bg-red-600/90 hover:bg-red-600 text-white shadow-sm hover:shadow transition-all"
            >
              <ShieldAlert className="w-4 h-4 animate-pulse" />
              <span className="hidden sm:inline">{t('emergencyContactsBtn')}</span>
              <span className="sm:hidden">{t('emergency')}</span>
            </button>
          </div>
        </div>

        {/* Navigation Tabs Bar */}
        <nav className="flex gap-1 sm:gap-2 overflow-x-auto pb-2 scrollbar-none pt-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs sm:text-sm font-medium whitespace-nowrap transition-all ${
                  isActive
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{item.label}</span>
                {item.badge !== undefined && (
                  <span className="bg-amber-500 text-slate-950 text-[10px] font-bold px-1.5 py-0.2 rounded-full">
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>
    </header>
  );
};

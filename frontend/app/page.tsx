'use client';

import { MarketOverview } from '@/components/dashboard/market-overview';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';

export default function DashboardPage() {
  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Dashboard</h1>
          <p className="text-gray-400 mt-1">Welcome back, Sentinel Commander.</p>
        </div>
        <Button className="bg-indigo-600 hover:bg-indigo-700">
          <Plus className="w-4 h-4 mr-2" />
          New Portfolio
        </Button>
      </div>

      <MarketOverview />

      {/* Placeholder for Portfolio Summary & Chart */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 h-96 rounded-xl border border-[#1E293B] bg-[#151B2B] p-6 flex items-center justify-center text-gray-500">
          Portfolio Value History Chart (Coming Soon)
        </div>
        <div className="h-96 rounded-xl border border-[#1E293B] bg-[#151B2B] p-6 flex items-center justify-center text-gray-500">
          Allocation Donut Chart (Coming Soon)
        </div>
      </div>
    </div>
  );
}

'use client';

import { PortfolioList } from '@/components/portfolio/portfolio-list';
import { PortfolioGrowthChart } from '@/components/portfolio/portfolio-growth-chart';
import { AssetAllocationChart } from '@/components/portfolio/asset-allocation-chart';

export default function PortfolioPage() {
    return (
        <div className="space-y-8">
            <div>
                <h1 className="text-3xl font-bold">My Portfolios</h1>
                <p className="text-gray-400 mt-1">Manage your crypto and stock holdings.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2">
                    <PortfolioGrowthChart />
                </div>
                <div>
                    <AssetAllocationChart />
                </div>
            </div>

            <PortfolioList />
        </div>
    );
}

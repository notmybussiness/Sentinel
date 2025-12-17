'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api/client';
import { Portfolio, AiAnalysisResponse, AiAnalysisType, AiServiceStatus } from '@/lib/api/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Brain, CheckCircle, AlertTriangle, TrendingUp, Shield, PieChart, Zap, Lightbulb, Target } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

const ANALYSIS_TYPES: { label: string; value: AiAnalysisType; icon: React.ReactNode; description: string }[] = [
    { label: 'Overview', value: 'OVERVIEW', icon: <PieChart className="w-5 h-5" />, description: 'General portfolio analysis' },
    { label: 'Risk', value: 'RISK', icon: <Shield className="w-5 h-5" />, description: 'Risk assessment' },
    { label: 'Diversification', value: 'DIVERSIFICATION', icon: <TrendingUp className="w-5 h-5" />, description: 'Diversification analysis' },
    { label: 'Performance', value: 'PERFORMANCE', icon: <Zap className="w-5 h-5" />, description: 'Performance evaluation' },
    { label: 'Recommendations', value: 'RECOMMENDATION', icon: <Target className="w-5 h-5" />, description: 'Investment suggestions' },
];

export default function AiPage() {
    const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
    const [selectedPortfolio, setSelectedPortfolio] = useState<number | null>(null);
    const [selectedType, setSelectedType] = useState<AiAnalysisType>('OVERVIEW');
    const [analysis, setAnalysis] = useState<AiAnalysisResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [aiStatus, setAiStatus] = useState<AiServiceStatus | null>(null);

    useEffect(() => {
        async function fetchData() {
            const [portfolioData, statusData] = await Promise.all([
                api.portfolio.getAll(),
                api.ai.getStatus(),
            ]);
            setPortfolios(portfolioData);
            setAiStatus(statusData);
            if (portfolioData.length > 0) {
                setSelectedPortfolio(portfolioData[0].id);
            }
        }
        fetchData();
    }, []);

    const handleAnalyze = async () => {
        if (!selectedPortfolio) return;

        setLoading(true);
        setAnalysis(null);

        const result = await api.ai.analyze({
            portfolioId: selectedPortfolio,
            analysisType: selectedType,
            includeMarketContext: true,
        });

        setAnalysis(result);
        setLoading(false);
    };

    const getRiskColor = (level: string) => {
        switch (level) {
            case 'LOW': return 'text-green-500 bg-green-500/10';
            case 'MEDIUM': return 'text-yellow-500 bg-yellow-500/10';
            case 'HIGH': return 'text-red-500 bg-red-500/10';
            default: return 'text-gray-500 bg-gray-500/10';
        }
    };

    const getDiversificationColor = (score: number) => {
        if (score >= 70) return 'text-green-500';
        if (score >= 40) return 'text-yellow-500';
        return 'text-red-500';
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold flex items-center gap-3">
                        <Brain className="w-8 h-8 text-indigo-500" />
                        AI Analysis
                    </h1>
                    <p className="text-gray-400 mt-1">Gemini AI-powered portfolio insights and recommendations</p>
                </div>
                {aiStatus && (
                    <div className={cn(
                        "flex items-center gap-2 px-3 py-1.5 rounded-full text-sm",
                        aiStatus.available ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500"
                    )}>
                        <div className={cn(
                            "w-2 h-2 rounded-full",
                            aiStatus.available ? "bg-green-500" : "bg-red-500"
                        )} />
                        {aiStatus.available ? 'AI Available' : 'AI Unavailable'}
                    </div>
                )}
            </div>

            {/* Controls */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Portfolio Selector */}
                <Card className="bg-[#1E293B]/50 border-none">
                    <CardHeader className="pb-3">
                        <CardTitle className="text-lg">Select Portfolio</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <select
                            value={selectedPortfolio || ''}
                            onChange={(e) => setSelectedPortfolio(Number(e.target.value))}
                            className="w-full px-4 py-3 bg-[#0B0F19] border border-[#1E293B] rounded-lg text-white focus:outline-none focus:border-indigo-500"
                        >
                            <option value="" disabled>Select a portfolio...</option>
                            {portfolios.map((p) => (
                                <option key={p.id} value={p.id}>
                                    {p.name} (${p.totalValue.toLocaleString()})
                                </option>
                            ))}
                        </select>
                    </CardContent>
                </Card>

                {/* Analysis Type */}
                <Card className="bg-[#1E293B]/50 border-none">
                    <CardHeader className="pb-3">
                        <CardTitle className="text-lg">Analysis Type</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="flex flex-wrap gap-2">
                            {ANALYSIS_TYPES.map((type) => (
                                <button
                                    key={type.value}
                                    onClick={() => setSelectedType(type.value)}
                                    className={cn(
                                        "flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors",
                                        selectedType === type.value
                                            ? "bg-indigo-600 text-white"
                                            : "bg-[#0B0F19] text-gray-400 hover:text-white hover:bg-[#0B0F19]/80"
                                    )}
                                >
                                    {type.icon}
                                    {type.label}
                                </button>
                            ))}
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* Analyze Button */}
            <button
                onClick={handleAnalyze}
                disabled={!selectedPortfolio || loading || !aiStatus?.available}
                className={cn(
                    "w-full py-4 rounded-lg font-bold text-lg transition-all flex items-center justify-center gap-3",
                    selectedPortfolio && aiStatus?.available && !loading
                        ? "bg-indigo-600 hover:bg-indigo-700 text-white"
                        : "bg-gray-700 text-gray-400 cursor-not-allowed"
                )}
            >
                {loading ? (
                    <>
                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                        Analyzing with Gemini AI...
                    </>
                ) : (
                    <>
                        <Brain className="w-6 h-6" />
                        Analyze Portfolio
                    </>
                )}
            </button>

            {/* Results */}
            {analysis && (
                <div className="space-y-6">
                    {/* Summary & Scores */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <Card className="bg-[#1E293B]/50 border-none md:col-span-2">
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <Lightbulb className="w-5 h-5 text-yellow-500" />
                                    Summary
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="text-gray-300 leading-relaxed">{analysis.summary}</p>
                            </CardContent>
                        </Card>

                        <div className="space-y-4">
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardContent className="p-4">
                                    <p className="text-xs text-gray-400 mb-1">Risk Level</p>
                                    <div className={cn(
                                        "inline-flex items-center gap-2 px-3 py-1 rounded-full font-bold",
                                        getRiskColor(analysis.riskLevel)
                                    )}>
                                        <Shield className="w-4 h-4" />
                                        {analysis.riskLevel}
                                    </div>
                                </CardContent>
                            </Card>
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardContent className="p-4">
                                    <p className="text-xs text-gray-400 mb-1">Diversification Score</p>
                                    <p className={cn(
                                        "text-3xl font-bold",
                                        getDiversificationColor(analysis.diversificationScore)
                                    )}>
                                        {analysis.diversificationScore.toFixed(0)}
                                        <span className="text-lg text-gray-500">/100</span>
                                    </p>
                                </CardContent>
                            </Card>
                        </div>
                    </div>

                    {/* Insights & Recommendations */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* Insights */}
                        <Card className="bg-[#1E293B]/50 border-none">
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <CheckCircle className="w-5 h-5 text-blue-500" />
                                    Key Insights
                                </CardTitle>
                                <CardDescription>AI-generated observations about your portfolio</CardDescription>
                            </CardHeader>
                            <CardContent>
                                <ul className="space-y-3">
                                    {analysis.insights.map((insight, i) => (
                                        <li key={i} className="flex items-start gap-3">
                                            <div className="w-6 h-6 rounded-full bg-blue-500/10 flex items-center justify-center text-blue-500 text-sm font-bold flex-shrink-0">
                                                {i + 1}
                                            </div>
                                            <p className="text-gray-300 text-sm">{insight}</p>
                                        </li>
                                    ))}
                                </ul>
                            </CardContent>
                        </Card>

                        {/* Recommendations */}
                        <Card className="bg-[#1E293B]/50 border-none">
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <AlertTriangle className="w-5 h-5 text-amber-500" />
                                    Recommendations
                                </CardTitle>
                                <CardDescription>Suggested actions to improve your portfolio</CardDescription>
                            </CardHeader>
                            <CardContent>
                                <ul className="space-y-3">
                                    {analysis.recommendations.map((rec, i) => (
                                        <li key={i} className="flex items-start gap-3">
                                            <div className="w-6 h-6 rounded-full bg-amber-500/10 flex items-center justify-center text-amber-500 text-sm font-bold flex-shrink-0">
                                                {i + 1}
                                            </div>
                                            <p className="text-gray-300 text-sm">{rec}</p>
                                        </li>
                                    ))}
                                </ul>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Timestamp */}
                    <p className="text-center text-gray-500 text-sm">
                        Analysis generated at {new Date(analysis.analyzedAt).toLocaleString()}
                    </p>
                </div>
            )}

            {/* Empty State */}
            {!analysis && !loading && (
                <Card className="bg-[#1E293B]/50 border-none">
                    <CardContent className="py-16 text-center">
                        <Brain className="w-16 h-16 mx-auto text-gray-600 mb-4" />
                        <h3 className="text-xl font-bold text-gray-400 mb-2">Ready for Analysis</h3>
                        <p className="text-gray-500 max-w-md mx-auto">
                            Select a portfolio and analysis type, then click "Analyze Portfolio" to get AI-powered insights.
                        </p>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}

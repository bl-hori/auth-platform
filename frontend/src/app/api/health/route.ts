/**
 * Health Check API Endpoint
 *
 * @description Simple health check endpoint for Kubernetes liveness/readiness probes
 */

import { NextResponse } from 'next/server'

/**
 * GET /api/health - Health check endpoint
 *
 * @returns JSON response with status and timestamp
 */
export async function GET() {
  return NextResponse.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    service: 'auth-platform-frontend',
  })
}

/**
 * Test data generators and fixtures for E2E tests
 */

export interface TestUser {
  email: string;
  name: string;
  status: 'ACTIVE' | 'INACTIVE';
  attributes: Record<string, unknown>;
}

export interface TestRole {
  name: string;
  description: string;
}

export interface TestPermission {
  name: string;
  resource: string;
  action: string;
  effect: 'ALLOW' | 'DENY';
}

/**
 * Generate a unique test user
 */
export function generateTestUser(overrides?: Partial<TestUser>): TestUser {
  const timestamp = Date.now();
  return {
    email: `test-user-${timestamp}@example.com`,
    name: `Test User ${timestamp}`,
    status: 'ACTIVE',
    attributes: {
      department: 'Engineering',
      location: 'Tokyo'
    },
    ...overrides,
  };
}

/**
 * Generate a unique test role
 */
export function generateTestRole(overrides?: Partial<TestRole>): TestRole {
  const timestamp = Date.now();
  return {
    name: `test-role-${timestamp}`,
    description: `Test role created at ${new Date(timestamp).toISOString()}`,
    ...overrides,
  };
}

/**
 * Generate a test permission
 */
export function generateTestPermission(overrides?: Partial<TestPermission>): TestPermission {
  const timestamp = Date.now();
  return {
    name: `test-permission-${timestamp}`,
    resource: 'test-resource',
    action: 'READ',
    effect: 'ALLOW',
    ...overrides,
  };
}

/**
 * Generate test policy (Rego)
 */
export function generateTestPolicy(name?: string): { name: string; content: string } {
  const timestamp = Date.now();
  const policyName = name || `test_policy_${timestamp}`;

  return {
    name: policyName,
    content: `package ${policyName}

# Test policy for E2E testing
# Auto-generated at ${new Date(timestamp).toISOString()}

default allow = false

# Allow read access to authenticated users
allow {
    input.action == "READ"
    input.user.authenticated == true
}

# Allow admin users all actions
allow {
    input.user.role == "ADMIN"
}
`,
  };
}

/**
 * Wait utility for async operations
 */
export function wait(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

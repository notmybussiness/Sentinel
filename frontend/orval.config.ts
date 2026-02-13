import { defineConfig } from 'orval';

export default defineConfig({
  sentinel: {
    input: '../docs/specs/api/openapi.json',
    output: {
      mode: 'single',
      target: './lib/api/generated/sdk.ts',
      schemas: './lib/api/generated/model',
      client: 'fetch',
      override: {
        mutator: {
          path: './lib/api/generated/mutator.ts',
          name: 'customFetch',
        },
      },
    },
  },
});

import path from 'node:path';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [
    {
      name: 'ped360-relative-test-includes',
      config(config) {
        const includes = config.test?.include;
        if (!includes) {
          return;
        }

        const root = config.root ?? process.cwd();
        config.test.include = includes.map((include) =>
          path.isAbsolute(include) ? path.relative(root, include).replaceAll('\\', '/') : include,
        );
      },
    },
  ],
});

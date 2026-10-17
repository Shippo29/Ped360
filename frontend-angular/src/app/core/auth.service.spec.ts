import { describe, expect, it } from 'vitest';
import { combinarRolesTokens } from './auth.service';

describe('combinarRolesTokens', () => {
  it('incluye roles del ID token cuando no hay roles en el access token', () => {
    expect(combinarRolesTokens({ roles: ['Operador'] }, {})).toEqual(['Operador']);
  });

  it('incluye roles del access token cuando no hay roles en el ID token', () => {
    expect(combinarRolesTokens({}, { roles: ['Admin'] })).toEqual(['Admin']);
  });

  it('combina ambos tokens sin duplicados', () => {
    expect(
      combinarRolesTokens({ roles: ['Operador', 'Cliente'] }, { roles: ['Operador', 'Admin'] })
    ).toEqual(['Operador', 'Cliente', 'Admin']);
  });

  it('devuelve una lista vacía si ninguno contiene roles', () => {
    expect(combinarRolesTokens({}, {})).toEqual([]);
  });

  it('descarta valores que no sean roles exactos permitidos', () => {
    expect(
      combinarRolesTokens(
        { roles: ['admin', 'Admin ', 'Cliente', 1] },
        { roles: ['Operador', 'Cliente', null] }
      )
    ).toEqual(['Cliente', 'Operador']);
  });
});

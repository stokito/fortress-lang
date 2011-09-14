/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

#include <stdio.h>
#include <fenv.h>
#include <math.h>

#pragma fenv-access

#define COMPUTE(exp, d, u) \
  do { \
    int temp = fegetround(); \
    fesetround(FE_DOWNWARD); \
    d = (exp); \
    fesetround(FE_UPWARD); \
    u = (exp); \
    fesetround(temp); \
    } while (0)

void print_float_example(float f1, float f2, float fd, float fu) {
  printf("      { (float)%16.9e, (float)%16.9e, (float)%16.9e, (float)%16.9e },%s\n",
         f1, f2, fd, fu, (fd == fu) ? " // =" : "");
}

void print_double_example(double d1, double d2, double dd, double du) {
  printf("      { %26.18e, %26.18e, %26.18e, %26.18e },%s\n",
         d1, d2, dd, du, (dd == du) ? " // =" : "");
}

void print_unary_float_example(float f1, float fd, float fu) {
  printf("      { (float)%16.9e, (float)%16.9e, (float)%16.9e },%s\n",
         f1, fd, fu, (fd == fu) ? " // =" : "");
}

void print_unary_double_example(double d1, double dd, double du) {
  printf("      { %26.18e, %26.18e, %26.18e },%s\n",
         d1, dd, du, (dd == du) ? " // =" : "");
}

void float_add_example(float f1, float f2) {
  float fd, fu;
  COMPUTE(f1 + f2, fd, fu);
  print_float_example(f1, f2, fd, fu);
}

void float_multiply_example(float f1, float f2) {
  float fd, fu;
  COMPUTE(f1 * f2, fd, fu);
  print_float_example(f1, f2, fd, fu);
}

void float_divide_example(float f1, float f2) {
  float fd, fu;
  COMPUTE(f1 / f2, fd, fu);
  print_float_example(f1, f2, fd, fu);
  /*  COMPUTE(f2 / f1, fd, fu);
      print_float_example(f2, f1, fd, fu); */
}

void float_sqrt_example(float f1) {
  float fd, fu;
  COMPUTE(sqrt(f1), fd, fu);
  print_unary_float_example(f1, fd, fu);
}

void double_add_example(double d1, double d2) {
  double dd, du;
  COMPUTE(d1 + d2, dd, du);
  print_double_example(d1, d2, dd, du);
}

void double_multiply_example(double d1, double d2) {
  double dd, du;
  COMPUTE(d1 * d2, dd, du);
  print_double_example(d1, d2, dd, du);
}

void double_divide_example(double d1, double d2) {
  double dd, du;
  COMPUTE(d1 / d2, dd, du);
  print_double_example(d1, d2, dd, du);
  COMPUTE(d2 / d1, dd, du);
  print_double_example(d2, d1, dd, du);
}

void double_sqrt_example(double d1) {
  double dd, du;
  COMPUTE(sqrt(d1), dd, du);
  print_unary_double_example(d1, dd, du);
}

int main(int argc, char *argv[]) {
  float finf = ((float) 1.0) / ((float) 0.0);
  float fnan = finf - finf;
  double dinf = 1.0 / 0.0;
  double dnan = dinf - dinf;
  double d_2_26 = 64.0 * 1024.0 * 1024.0;
  double d_2_55 = 8.0 * d_2_26 * d_2_26;
  float f_2_26 = (float) d_2_26;
  float ftiny = 1.0;
  double dtiny = 1.0;
  float fsemihuge = 1.0, fhuge, ftemp;
  double dsemihuge = 1.0, dhuge, dtemp;
  int j, k;
  while ((ftiny / ((float) 2.0)) != 0) ftiny /= (float) 2.0;
  while ((dtiny / 2.0) != 0) dtiny /= 2.0;
  while ((fsemihuge * ((float) 2.0)) != finf) fsemihuge *= (float) 2.0;
  while ((dsemihuge * 2.0) != dinf) dsemihuge *= 2.0;
  for (ftemp = fsemihuge; (fhuge + ftemp) != finf; ftemp /= (float) 2.0) fhuge += ftemp;
  for (dtemp = dsemihuge; (dhuge + dtemp) != dinf; dtemp /= 2.0) dhuge += dtemp;
  printf("/*******************************************************************************\n");
  printf("    Copyright 2008, Oracle and/or its affiliates.\n");
  printf("    All rights reserved.\n\n");
  printf("    Use is subject to license terms.\n\n");
  printf("    This distribution may include materials developed by third parties.\n\n");
  printf(" ******************************************************************************/\n\n");
  printf("// This file is generated automatically by compiling and executing\n");
  printf("// the C program directedtestgen.c .\n");
  printf("\n");
  printf("package com.sun.fortress.numerics;\n");
  printf("\n");
  printf("class DirectedRoundingTestData {\n");
  printf("\n");
  printf("  private static double inf = 1.0 / 0.0;\n");
  printf("  private static double nan = inf - inf;\n");
  printf("\n");
  printf("  public static float[][] float_add_data = make_float_add_data();\n");
  printf("\n");
  printf("  public static float[][] make_float_add_data() {\n");
  printf("    float[][] result = {\n");
  float_add_example(fnan, fnan);
  float_add_example(fnan, finf);
  float_add_example(fnan, -finf);
  float_add_example(fnan, (float) 0.0);
  float_add_example(fnan, -(float) 0.0);
  float_add_example(fnan, (float) 1.0);
  float_add_example(fnan, -(float) 1.0);
  float_add_example(finf, finf);
  float_add_example(finf, -finf);
  float_add_example(finf, (float) 0.0);
  float_add_example(finf, -(float) 0.0);
  float_add_example(finf, (float) 1.0);
  float_add_example(finf, -(float) 1.0);
  float_add_example(-finf, -finf);
  float_add_example(-finf, (float) 0.0);
  float_add_example(-finf, -(float) 0.0);
  float_add_example(-finf, (float) 1.0);
  float_add_example(-finf, -(float) 1.0);
  float_add_example((float) 0.0, (float) 0.0);
  float_add_example((float) 0.0, -(float) 0.0);
  float_add_example((float) 0.0, (float) 1.0);
  float_add_example((float) 0.0, -(float) 1.0);
  float_add_example(-(float) 0.0, -(float) 0.0);
  float_add_example(-(float) 0.0, (float) 1.0);
  float_add_example(-(float) 0.0, -(float) 1.0);
  float_add_example((float) 1.0, (float) 1.0);
  float_add_example((float) 1.0, -(float) 1.0);
  float_add_example(-(float) 1.0, -(float) 1.0);
  for (k = 1; k < 16; k++) {
    float_add_example((float) 1.0, k / f_2_26);
  }
  for (j = 1; j < 7; j++) {
    for (k = 1; k < 7; k++) {
      float_add_example(((float) 1.0) / (2 * j + 1), ((float) 1.0) / (2 * k + 35));
    }
  }
  float_add_example((float) 43.0, ftiny);
  float_add_example((float) 43.0, -ftiny);
  float_add_example(-(float) 43.0, ftiny);
  float_add_example(-(float) 43.0, -ftiny);
  float_add_example(ftiny, ftiny);
  float_add_example(ftiny, -ftiny);
  float_add_example(-ftiny, -ftiny);
  float_add_example(43 * ftiny, ftiny);
  float_add_example(43 * ftiny, -ftiny);
  float_add_example(-43 * ftiny, ftiny);
  float_add_example(-43 * ftiny, -ftiny);
  float_add_example(fhuge, fhuge);
  float_add_example(fhuge, -fhuge);
  float_add_example(-fhuge, -fhuge);
  float_add_example(fsemihuge, fsemihuge);
  float_add_example(fsemihuge, -fsemihuge);
  float_add_example(-fsemihuge, -fsemihuge);
  float_add_example(fhuge, fsemihuge);
  float_add_example(fhuge, -fsemihuge);
  float_add_example(-fhuge, fsemihuge);
  float_add_example(-fhuge, -fsemihuge);
  float_add_example(fhuge, (float) 1.0);
  float_add_example(fhuge, -(float) 1.0);
  float_add_example(-fhuge, (float) 1.0);
  float_add_example(-fhuge, -(float) 1.0);
  float_add_example(fhuge, ftiny);
  float_add_example(fhuge, -ftiny);
  float_add_example(-fhuge, ftiny);
  float_add_example(-fhuge, -ftiny);
  float_add_example(fsemihuge, ftiny);
  float_add_example(fsemihuge, -ftiny);
  float_add_example(-fsemihuge, ftiny);
  float_add_example(-fsemihuge, -ftiny);
  for (k = 1; k < 16; k++) {
    float_add_example(fhuge, (k / f_2_26) * fsemihuge);
  }
  for (k = 1; k < 16; k++) {
    float_add_example(fsemihuge, (k / f_2_26) * fsemihuge);
  }
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("  public static float[][] float_multiply_data = make_float_multiply_data();\n");
  printf("\n");
  printf("  public static float[][] make_float_multiply_data() {\n");
  printf("    float[][] result = {\n");
  float_multiply_example(fnan, fnan);
  float_multiply_example(fnan, finf);
  float_multiply_example(fnan, -finf);
  float_multiply_example(fnan, (float) 0.0);
  float_multiply_example(fnan, -(float) 0.0);
  float_multiply_example(fnan, (float) 1.0);
  float_multiply_example(fnan, -(float) 1.0);
  float_multiply_example(finf, finf);
  float_multiply_example(finf, -finf);
  float_multiply_example(finf, (float) 0.0);
  float_multiply_example(finf, -(float) 0.0);
  float_multiply_example(finf, (float) 1.0);
  float_multiply_example(finf, -(float) 1.0);
  float_multiply_example(-finf, -finf);
  float_multiply_example(-finf, (float) 0.0);
  float_multiply_example(-finf, -(float) 0.0);
  float_multiply_example(-finf, (float) 1.0);
  float_multiply_example(-finf, -(float) 1.0);
  float_multiply_example((float) 0.0, (float) 0.0);
  float_multiply_example((float) 0.0, -(float) 0.0);
  float_multiply_example((float) 0.0, (float) 1.0);
  float_multiply_example((float) 0.0, -(float) 1.0);
  float_multiply_example(-(float) 0.0, -(float) 0.0);
  float_multiply_example(-(float) 0.0, (float) 1.0);
  float_multiply_example(-(float) 0.0, -(float) 1.0);
  float_multiply_example((float) 1.0, (float) 1.0);
  float_multiply_example((float) 1.0, -(float) 1.0);
  float_multiply_example(-(float) 1.0, -(float) 1.0);
  for (k = 0; k < 13; k++) {
    float p = (float) (123456 >> k);
    float q = (float) (123456 >> (k + 1));
    float r = (float) (135791 >> k);
    float_multiply_example(p, r);
    float_multiply_example(q, r);
    float_multiply_example(-p, r);
    float_multiply_example(-q, r);
    float_multiply_example(p, -r);
    float_multiply_example(q, -r);
    float_multiply_example(-p, -r);
    float_multiply_example(-q, -r);
  }
  for (j = 1; j < 7; j++) {
    for (k = 1; k < 7; k++) {
      float_multiply_example(((float) 1.0) / (2 * j + 1), ((float) 1.0) / (2 * k + 35));
      float_multiply_example(-((float) 1.0) / (2 * j + 1), ((float) 1.0) / (2 * k + 35));
      float_multiply_example(((float) 1.0) / (2 * j + 1), -((float) 1.0) / (2 * k + 35));
      float_multiply_example(-((float) 1.0) / (2 * j + 1), -((float) 1.0) / (2 * k + 35));
    }
  }
  float_multiply_example((float) 43.0, ftiny);
  float_multiply_example((float) 43.0, -ftiny);
  float_multiply_example(-(float) 43.0, ftiny);
  float_multiply_example(-(float) 43.0, -ftiny);
  float_multiply_example((float) 0.25, ftiny);
  float_multiply_example((float) 0.25, -ftiny);
  float_multiply_example(-(float) 0.25, ftiny);
  float_multiply_example(-(float) 0.25, -ftiny);
  float_multiply_example(ftiny, ftiny);
  float_multiply_example(-ftiny, ftiny);
  float_multiply_example(-ftiny, -ftiny);
  float_multiply_example(fhuge, fhuge);
  float_multiply_example(fhuge, -fhuge);
  float_multiply_example(-fhuge, -fhuge);
  float_multiply_example(fsemihuge, fsemihuge);
  float_multiply_example(fsemihuge, -fsemihuge);
  float_multiply_example(-fsemihuge, -fsemihuge);
  float_multiply_example(fhuge, fsemihuge);
  float_multiply_example(fhuge, -fsemihuge);
  float_multiply_example(-fhuge, fsemihuge);
  float_multiply_example(-fhuge, -fsemihuge);
  float_multiply_example(fhuge, (float) 0.99);
  float_multiply_example(fhuge, -(float) 0.99);
  float_multiply_example(-fhuge, (float) 0.99);
  float_multiply_example(-fhuge, -(float) 0.99);
  float_multiply_example(fhuge, (float) 1.0);
  float_multiply_example(fhuge, -(float) 1.0);
  float_multiply_example(-fhuge, (float) 1.0);
  float_multiply_example(-fhuge, -(float) 1.0);
  float_multiply_example(fhuge, (float) 1.01);
  float_multiply_example(fhuge, -(float) 1.01);
  float_multiply_example(-fhuge, (float) 1.01);
  float_multiply_example(-fhuge, -(float) 1.01);
  float_multiply_example(fsemihuge, (float) 1.99);
  float_multiply_example(fsemihuge, -(float) 1.99);
  float_multiply_example(-fsemihuge, (float) 1.99);
  float_multiply_example(-fsemihuge, -(float) 1.99);
  float_multiply_example(fsemihuge, (float) 2.01);
  float_multiply_example(fsemihuge, -(float) 2.01);
  float_multiply_example(-fsemihuge, (float) 2.01);
  float_multiply_example(-fsemihuge, -(float) 2.01);
  float_multiply_example(fhuge, ftiny);
  float_multiply_example(fhuge, -ftiny);
  float_multiply_example(-fhuge, ftiny);
  float_multiply_example(-fhuge, -ftiny);
  float_multiply_example(fsemihuge, ftiny);
  float_multiply_example(fsemihuge, -ftiny);
  float_multiply_example(-fsemihuge, ftiny);
  float_multiply_example(-fsemihuge, -ftiny);
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("  public static float[][] float_divide_data = make_float_divide_data();\n");
  printf("\n");
  printf("  public static float[][] make_float_divide_data() {\n");
  printf("    float[][] result = {\n");
  float_divide_example(fnan, fnan);
  float_divide_example(fnan, finf);
  float_divide_example(fnan, -finf);
  float_divide_example(fnan, (float) 0.0);
  float_divide_example(fnan, -(float) 0.0);
  float_divide_example(fnan, (float) 1.0);
  float_divide_example(fnan, -(float) 1.0);
  float_divide_example(finf, finf);
  float_divide_example(finf, -finf);
  float_divide_example(finf, (float) 0.0);
  float_divide_example(finf, -(float) 0.0);
  float_divide_example(finf, (float) 1.0);
  float_divide_example(finf, -(float) 1.0);
  float_divide_example(-finf, -finf);
  float_divide_example(-finf, (float) 0.0);
  float_divide_example(-finf, -(float) 0.0);
  float_divide_example(-finf, (float) 1.0);
  float_divide_example(-finf, -(float) 1.0);
  float_divide_example((float) 0.0, (float) 0.0);
  float_divide_example((float) 0.0, -(float) 0.0);
  float_divide_example((float) 0.0, (float) 1.0);
  float_divide_example((float) 0.0, -(float) 1.0);
  float_divide_example(-(float) 0.0, -(float) 0.0);
  float_divide_example(-(float) 0.0, (float) 1.0);
  float_divide_example(-(float) 0.0, -(float) 1.0);
  float_divide_example((float) 1.0, (float) 1.0);
  float_divide_example((float) 1.0, -(float) 1.0);
  float_divide_example(-(float) 1.0, -(float) 1.0);
  for (k = 0; k < 13; k++) {
    float p = (float) (123456 >> k);
    float q = (float) (123456 >> (k + 1));
    float r = (float) (135791 >> k);
    float_divide_example(p*r, r);
    float_divide_example(q*r, r);
    float_divide_example(-p*r, r);
    float_divide_example(-q*r, r);
    float_divide_example(p*r, -r);
    float_divide_example(q*r, -r);
    float_divide_example(-p*r, -r);
    float_divide_example(-q*r, -r);
 }
  for (j = 1; j < 7; j++) {
    for (k = 1; k < 7; k++) {
      float_divide_example(((float) 1.0) / (2 * j + 1), ((float) 1.0) / (2 * k + 35));
      float_divide_example(-((float) 1.0) / (2 * j + 1), ((float) 1.0) / (2 * k + 35));
      float_divide_example(((float) 1.0) / (2 * j + 1), -((float) 1.0) / (2 * k + 35));
      float_divide_example(-((float) 1.0) / (2 * j + 1), -((float) 1.0) / (2 * k + 35));
    }
  }
  float_divide_example((float) 43.0, ftiny);
  float_divide_example((float) 43.0, -ftiny);
  float_divide_example(-(float) 43.0, ftiny);
  float_divide_example(-(float) 43.0, -ftiny);
  float_divide_example((float) 0.25, ftiny);
  float_divide_example((float) 0.25, -ftiny);
  float_divide_example(-(float) 0.25, ftiny);
  float_divide_example(-(float) 0.25, -ftiny);
  float_divide_example(ftiny, ftiny);
  float_divide_example(-ftiny, ftiny);
  float_divide_example(ftiny, -ftiny);
  float_divide_example(-ftiny, -ftiny);
  float_divide_example(fhuge, fhuge);
  float_divide_example(fhuge, -fhuge);
  float_divide_example(-fhuge, -fhuge);
  float_divide_example(fsemihuge, fsemihuge);
  float_divide_example(fsemihuge, -fsemihuge);
  float_divide_example(-fsemihuge, -fsemihuge);
  float_divide_example(fhuge, fsemihuge);
  float_divide_example(fhuge, -fsemihuge);
  float_divide_example(-fhuge, fsemihuge);
  float_divide_example(-fhuge, -fsemihuge);
  float_divide_example(fhuge, (float) 0.99);
  float_divide_example(fhuge, -(float) 0.99);
  float_divide_example(-fhuge, (float) 0.99);
  float_divide_example(-fhuge, -(float) 0.99);
  float_divide_example(fhuge, (float) 1.0);
  float_divide_example(fhuge, -(float) 1.0);
  float_divide_example(-fhuge, (float) 1.0);
  float_divide_example(-fhuge, -(float) 1.0);
  float_divide_example(fhuge, (float) 1.01);
  float_divide_example(fhuge, -(float) 1.01);
  float_divide_example(-fhuge, (float) 1.01);
  float_divide_example(-fhuge, -(float) 1.01);
  float_divide_example(fhuge, fhuge);
  float_divide_example(fhuge, -fhuge);
  float_divide_example(-fhuge, -fhuge);
  float_divide_example(fsemihuge, fsemihuge);
  float_divide_example(fsemihuge, -fsemihuge);
  float_divide_example(-fsemihuge, -fsemihuge);
  float_divide_example(fhuge, fsemihuge);
  float_divide_example(fhuge, -fsemihuge);
  float_divide_example(-fhuge, fsemihuge);
  float_divide_example(-fhuge, -fsemihuge);
  float_divide_example(fhuge, (float) 0.99);
  float_divide_example(fhuge, -(float) 0.99);
  float_divide_example(-fhuge, (float) 0.99);
  float_divide_example(-fhuge, -(float) 0.99);
  float_divide_example(fhuge, (float) 1.0);
  float_divide_example(fhuge, -(float) 1.0);
  float_divide_example(-fhuge, (float) 1.0);
  float_divide_example(-fhuge, -(float) 1.0);
  float_divide_example(fhuge, (float) 1.01);
  float_divide_example(fhuge, -(float) 1.01);
  float_divide_example(-fhuge, (float) 1.01);
  float_divide_example(-fhuge, -(float) 1.01);
  float_divide_example(fsemihuge, (float) 1.99);
  float_divide_example(fsemihuge, -(float) 1.99);
  float_divide_example(-fsemihuge, (float) 1.99);
  float_divide_example(-fsemihuge, -(float) 1.99);
  float_divide_example(fsemihuge, (float) 2.01);
  float_divide_example(fsemihuge, -(float) 2.01);
  float_divide_example(-fsemihuge, (float) 2.01);
  float_divide_example(-fsemihuge, -(float) 2.01);
  float_divide_example(fhuge, ftiny);
  float_divide_example(fhuge, -ftiny);
  float_divide_example(-fhuge, ftiny);
  float_divide_example(-fhuge, -ftiny);
  float_divide_example(fsemihuge, ftiny);
  float_divide_example(fsemihuge, -ftiny);
  float_divide_example(-fsemihuge, ftiny);
  float_divide_example(-fsemihuge, -ftiny);
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("  public static float[][] float_sqrt_data = make_float_sqrt_data();\n");
  printf("\n");
  printf("  public static float[][] make_float_sqrt_data() {\n");
  printf("    float[][] result = {\n");
  float_sqrt_example(fnan);
  float_sqrt_example(finf);
  float_sqrt_example(-finf);
  float_sqrt_example((float) 0.0);
  float_sqrt_example((float) -0.0);
  float_sqrt_example((float) 1.0);
  float_sqrt_example((float) -1.0);
  for (k = 2; k < 38; k++) {
    float_sqrt_example((float) k);
  }
  for (k = 0; k < 13; k++) {
    float p = (float) (123456 >> k);
    float q = (float) (111111 >> k);
    float r = (float) (135791 >> k);
    float_sqrt_example(p);
    float_sqrt_example(q);
    float_sqrt_example(r);
    float_sqrt_example(p*p);
    float_sqrt_example(q*q);
    float_sqrt_example(r*r);
    float_sqrt_example(p*r);
 }
  for (k = 1; k < 7; k++) {
    float_sqrt_example(((float) 1.0) / (2 * j + 1));
    float_sqrt_example(((float) 1.0) / (2 * k + 35));
  }
  for (k = 2; k < 38; k++) {
    float_sqrt_example(ftiny * (float) k);
  }
  float_sqrt_example(ftiny);
  float_sqrt_example(-ftiny);
  float_sqrt_example(ftiny * (float) 2.0);
  float_sqrt_example(-ftiny * (float) 2.0);
  float_sqrt_example(fhuge);
  float_sqrt_example(-fhuge);
  float_sqrt_example(fhuge / (float) 2.0);
  float_sqrt_example(-fhuge / (float) 2.0);
  float_sqrt_example(fsemihuge);
  float_sqrt_example(-fsemihuge);
  float_sqrt_example(fsemihuge / (float) 2.0);
  float_sqrt_example(-fsemihuge / (float) 2.0);
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("  public static double[][] double_add_data = make_double_add_data();\n");
  printf("\n");
  printf("  public static double[][] make_double_add_data() {\n");
  printf("    double[][] result = {\n");
  double_add_example(dnan, dnan);
  double_add_example(dnan, dinf);
  double_add_example(dnan, -dinf);
  double_add_example(dnan, 0.0);
  double_add_example(dnan, -0.0);
  double_add_example(dnan, 1.0);
  double_add_example(dnan, -1.0);
  double_add_example(dinf, dinf);
  double_add_example(dinf, -dinf);
  double_add_example(dinf, 0.0);
  double_add_example(dinf, -0.0);
  double_add_example(dinf, 1.0);
  double_add_example(dinf, -1.0);
  double_add_example(-dinf, -dinf);
  double_add_example(-dinf, 0.0);
  double_add_example(-dinf, -0.0);
  double_add_example(-dinf, 1.0);
  double_add_example(-dinf, -1.0);
  double_add_example(0.0, 0.0);
  double_add_example(0.0, -0.0);
  double_add_example(0.0, 1.0);
  double_add_example(0.0, -1.0);
  double_add_example(-0.0, -0.0);
  double_add_example(-0.0, 1.0);
  double_add_example(-0.0, -1.0);
  double_add_example(1.0, 1.0);
  double_add_example(1.0, -1.0);
  double_add_example(-1.0, -1.0);
  for (k = 1; k < 16; k++) {
    double_add_example(1.0, k / d_2_55);
  }
  for (j = 1; j < 7; j++) {
    for (k = 1; k < 7; k++) {
      double_add_example((1.0) / (2 * j + 1), (1.0) / (2 * k + 35));
    }
  }
  double_add_example(43.0, dtiny);
  double_add_example(43.0, -dtiny);
  double_add_example(-43.0, dtiny);
  double_add_example(-43.0, -dtiny);
  double_add_example(dtiny, dtiny);
  double_add_example(dtiny, -dtiny);
  double_add_example(-dtiny, -dtiny);
  double_add_example(43 * dtiny, dtiny);
  double_add_example(43 * dtiny, -dtiny);
  double_add_example(-43 * dtiny, dtiny);
  double_add_example(-43 * dtiny, -dtiny);
  double_add_example(dhuge, dhuge);
  double_add_example(dhuge, -dhuge);
  double_add_example(-dhuge, -dhuge);
  double_add_example(dsemihuge, dsemihuge);
  double_add_example(dsemihuge, -dsemihuge);
  double_add_example(-dsemihuge, -dsemihuge);
  double_add_example(dhuge, dsemihuge);
  double_add_example(dhuge, -dsemihuge);
  double_add_example(-dhuge, dsemihuge);
  double_add_example(-dhuge, -dsemihuge);
  double_add_example(dhuge, 1.0);
  double_add_example(dhuge, -1.0);
  double_add_example(-dhuge, 1.0);
  double_add_example(-dhuge, -1.0);
  double_add_example(dhuge, dtiny);
  double_add_example(dhuge, -dtiny);
  double_add_example(-dhuge, dtiny);
  double_add_example(-dhuge, -dtiny);
  double_add_example(dsemihuge, dtiny);
  double_add_example(dsemihuge, -dtiny);
  double_add_example(-dsemihuge, dtiny);
  double_add_example(-dsemihuge, -dtiny);
  for (k = 1; k < 16; k++) {
    double_add_example(dhuge, (k / d_2_55) * dsemihuge);
  }
  for (k = 1; k < 16; k++) {
    double_add_example(dsemihuge, (k / d_2_55) * dsemihuge);
  }
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("  public static double[][] double_multiply_data = make_double_multiply_data();\n");
  printf("\n");
  printf("  public static double[][] make_double_multiply_data() {\n");
  printf("    double[][] result = {\n");
  double_multiply_example(dnan, dnan);
  double_multiply_example(dnan, dinf);
  double_multiply_example(dnan, -dinf);
  double_multiply_example(dnan, 0.0);
  double_multiply_example(dnan, -0.0);
  double_multiply_example(dnan, 1.0);
  double_multiply_example(dnan, -1.0);
  double_multiply_example(dinf, dinf);
  double_multiply_example(dinf, -dinf);
  double_multiply_example(dinf, 0.0);
  double_multiply_example(dinf, -0.0);
  double_multiply_example(dinf, 1.0);
  double_multiply_example(dinf, -1.0);
  double_multiply_example(-dinf, -dinf);
  double_multiply_example(-dinf, 0.0);
  double_multiply_example(-dinf, -0.0);
  double_multiply_example(-dinf, 1.0);
  double_multiply_example(-dinf, -1.0);
  double_multiply_example(0.0, 0.0);
  double_multiply_example(0.0, -0.0);
  double_multiply_example(0.0, 1.0);
  double_multiply_example(0.0, -1.0);
  double_multiply_example(-0.0, -0.0);
  double_multiply_example(-0.0, 1.0);
  double_multiply_example(-0.0, -1.0);
  double_multiply_example(1.0, 1.0);
  double_multiply_example(1.0, -1.0);
  double_multiply_example(-1.0, -1.0);
  for (k = 0; k < 13; k++) {
    double p = (1234567890 >> k);
    double q = (1234567890 >> (k + 1));
    double r = (1357915397 >> k);
    double_multiply_example(p, r);
    double_multiply_example(q, r);
    double_multiply_example(-p, r);
    double_multiply_example(-q, r);
    double_multiply_example(p, -r);
    double_multiply_example(q, -r);
    double_multiply_example(-p, -r);
    double_multiply_example(-q, -r);
  }
  for (j = 1; j < 7; j++) {
    for (k = 1; k < 7; k++) {
      double_multiply_example((1.0) / (2 * j + 1), (1.0) / (2 * k + 35));
      double_multiply_example(-(1.0) / (2 * j + 1), (1.0) / (2 * k + 35));
      double_multiply_example((1.0) / (2 * j + 1), -(1.0) / (2 * k + 35));
      double_multiply_example(-(1.0) / (2 * j + 1), -(1.0) / (2 * k + 35));
    }
  }
  double_multiply_example(43.0, dtiny);
  double_multiply_example(43.0, -dtiny);
  double_multiply_example(-43.0, dtiny);
  double_multiply_example(-43.0, -dtiny);
  double_multiply_example(0.25, dtiny);
  double_multiply_example(0.25, -dtiny);
  double_multiply_example(-0.25, dtiny);
  double_multiply_example(-0.25, -dtiny);
  double_multiply_example(dtiny, dtiny);
  double_multiply_example(-dtiny, dtiny);
  double_multiply_example(-dtiny, -dtiny);
  double_multiply_example(dhuge, dhuge);
  double_multiply_example(dhuge, -dhuge);
  double_multiply_example(-dhuge, -dhuge);
  double_multiply_example(dsemihuge, dsemihuge);
  double_multiply_example(dsemihuge, -dsemihuge);
  double_multiply_example(-dsemihuge, -dsemihuge);
  double_multiply_example(dhuge, dsemihuge);
  double_multiply_example(dhuge, -dsemihuge);
  double_multiply_example(-dhuge, dsemihuge);
  double_multiply_example(-dhuge, -dsemihuge);
  double_multiply_example(dhuge, 0.99);
  double_multiply_example(dhuge, -0.99);
  double_multiply_example(-dhuge, 0.99);
  double_multiply_example(-dhuge, -0.99);
  double_multiply_example(dhuge, 1.0);
  double_multiply_example(dhuge, -1.0);
  double_multiply_example(-dhuge, 1.0);
  double_multiply_example(-dhuge, -1.0);
  double_multiply_example(dhuge, 1.01);
  double_multiply_example(dhuge, -1.01);
  double_multiply_example(-dhuge, 1.01);
  double_multiply_example(-dhuge, -1.01);
  double_multiply_example(dsemihuge, 1.99);
  double_multiply_example(dsemihuge, -1.99);
  double_multiply_example(-dsemihuge, 1.99);
  double_multiply_example(-dsemihuge, -1.99);
  double_multiply_example(dsemihuge, 2.01);
  double_multiply_example(dsemihuge, -2.01);
  double_multiply_example(-dsemihuge, 2.01);
  double_multiply_example(-dsemihuge, -2.01);
  double_multiply_example(dhuge, dtiny);
  double_multiply_example(dhuge, -dtiny);
  double_multiply_example(-dhuge, dtiny);
  double_multiply_example(-dhuge, -dtiny);
  double_multiply_example(dsemihuge, dtiny);
  double_multiply_example(dsemihuge, -dtiny);
  double_multiply_example(-dsemihuge, dtiny);
  double_multiply_example(-dsemihuge, -dtiny);
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("  public static double[][] double_divide_data = make_double_divide_data();\n");
  printf("\n");
  printf("  public static double[][] make_double_divide_data() {\n");
  printf("    double[][] result = {\n");
  double_divide_example(dnan, dnan);
  double_divide_example(dnan, dinf);
  double_divide_example(dnan, -dinf);
  double_divide_example(dnan, 0.0);
  double_divide_example(dnan, -0.0);
  double_divide_example(dnan, 1.0);
  double_divide_example(dnan, -1.0);
  double_divide_example(dinf, dinf);
  double_divide_example(dinf, -dinf);
  double_divide_example(dinf, 0.0);
  double_divide_example(dinf, -0.0);
  double_divide_example(dinf, 1.0);
  double_divide_example(dinf, -1.0);
  double_divide_example(-dinf, -dinf);
  double_divide_example(-dinf, 0.0);
  double_divide_example(-dinf, -0.0);
  double_divide_example(-dinf, 1.0);
  double_divide_example(-dinf, -1.0);
  double_divide_example(0.0, 0.0);
  double_divide_example(0.0, -0.0);
  double_divide_example(0.0, 1.0);
  double_divide_example(0.0, -1.0);
  double_divide_example(-0.0, -0.0);
  double_divide_example(-0.0, 1.0);
  double_divide_example(-0.0, -1.0);
  double_divide_example(1.0, 1.0);
  double_divide_example(1.0, -1.0);
  double_divide_example(-1.0, -1.0);
  for (k = 0; k < 13; k++) {
    double p = (1234567890 >> k);
    double q = (1234567890 >> (k + 1));
    double r = (1357915397 >> k);
    double_divide_example(p*r, r);
    double_divide_example(q*r, r);
    double_divide_example(-p*r, r);
    double_divide_example(-q*r, r);
    double_divide_example(p*r, -r);
    double_divide_example(q*r, -r);
    double_divide_example(-p*r, -r);
    double_divide_example(-q*r, -r);
 }
  for (j = 1; j < 7; j++) {
    for (k = 1; k < 7; k++) {
      double_divide_example((1.0) / (2 * j + 1), (1.0) / (2 * k + 35));
      double_divide_example(-(1.0) / (2 * j + 1), (1.0) / (2 * k + 35));
      double_divide_example((1.0) / (2 * j + 1), -(1.0) / (2 * k + 35));
      double_divide_example(-(1.0) / (2 * j + 1), -(1.0) / (2 * k + 35));
    }
  }
  double_divide_example(43.0, dtiny);
  double_divide_example(43.0, -dtiny);
  double_divide_example(-43.0, dtiny);
  double_divide_example(-43.0, -dtiny);
  double_divide_example(0.25, dtiny);
  double_divide_example(0.25, -dtiny);
  double_divide_example(-0.25, dtiny);
  double_divide_example(-0.25, -dtiny);
  double_divide_example(dtiny, dtiny);
  double_divide_example(-dtiny, dtiny);
  double_divide_example(dtiny, -dtiny);
  double_divide_example(-dtiny, -dtiny);
  double_divide_example(dhuge, dhuge);
  double_divide_example(dhuge, -dhuge);
  double_divide_example(-dhuge, -dhuge);
  double_divide_example(dsemihuge, dsemihuge);
  double_divide_example(dsemihuge, -dsemihuge);
  double_divide_example(-dsemihuge, -dsemihuge);
  double_divide_example(dhuge, dsemihuge);
  double_divide_example(dhuge, -dsemihuge);
  double_divide_example(-dhuge, dsemihuge);
  double_divide_example(-dhuge, -dsemihuge);
  double_divide_example(dhuge, 0.99);
  double_divide_example(dhuge, -0.99);
  double_divide_example(-dhuge, 0.99);
  double_divide_example(-dhuge, -0.99);
  double_divide_example(dhuge, 1.0);
  double_divide_example(dhuge, -1.0);
  double_divide_example(-dhuge, 1.0);
  double_divide_example(-dhuge, -1.0);
  double_divide_example(dhuge, 1.01);
  double_divide_example(dhuge, -1.01);
  double_divide_example(-dhuge, 1.01);
  double_divide_example(-dhuge, -1.01);
  double_divide_example(dhuge, dhuge);
  double_divide_example(dhuge, -dhuge);
  double_divide_example(-dhuge, -dhuge);
  double_divide_example(dsemihuge, dsemihuge);
  double_divide_example(dsemihuge, -dsemihuge);
  double_divide_example(-dsemihuge, -dsemihuge);
  double_divide_example(dhuge, dsemihuge);
  double_divide_example(dhuge, -dsemihuge);
  double_divide_example(-dhuge, dsemihuge);
  double_divide_example(-dhuge, -dsemihuge);
  double_divide_example(dhuge, 0.99);
  double_divide_example(dhuge, -0.99);
  double_divide_example(-dhuge, 0.99);
  double_divide_example(-dhuge, -0.99);
  double_divide_example(dhuge, 1.0);
  double_divide_example(dhuge, -1.0);
  double_divide_example(-dhuge, 1.0);
  double_divide_example(-dhuge, -1.0);
  double_divide_example(dhuge, 1.01);
  double_divide_example(dhuge, -1.01);
  double_divide_example(-dhuge, 1.01);
  double_divide_example(-dhuge, -1.01);
  double_divide_example(dsemihuge, 1.99);
  double_divide_example(dsemihuge, -1.99);
  double_divide_example(-dsemihuge, 1.99);
  double_divide_example(-dsemihuge, -1.99);
  double_divide_example(dsemihuge, 2.01);
  double_divide_example(dsemihuge, -2.01);
  double_divide_example(-dsemihuge, 2.01);
  double_divide_example(-dsemihuge, -2.01);
  double_divide_example(dhuge, dtiny);
  double_divide_example(dhuge, -dtiny);
  double_divide_example(-dhuge, dtiny);
  double_divide_example(-dhuge, -dtiny);
  double_divide_example(dsemihuge, dtiny);
  double_divide_example(dsemihuge, -dtiny);
  double_divide_example(-dsemihuge, dtiny);
  double_divide_example(-dsemihuge, -dtiny);
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("  public static double[][] double_sqrt_data = make_double_sqrt_data();\n");
  printf("\n");
  printf("  public static double[][] make_double_sqrt_data() {\n");
  printf("    double[][] result = {\n");
  double_sqrt_example(dnan);
  double_sqrt_example(dinf);
  double_sqrt_example(-dinf);
  double_sqrt_example(0.0);
  double_sqrt_example(-0.0);
  double_sqrt_example(1.0);
  double_sqrt_example(-1.0);
  for (k = 2; k < 38; k++) {
    double_sqrt_example(k);
  }
  for (k = 0; k < 13; k++) {
    double p = (1234567890 >> k);
    double q = (1111111111 >> k);
    double r = (1357915397 >> k);
    double_sqrt_example(p);
    double_sqrt_example(q);
    double_sqrt_example(r);
    double_sqrt_example(p*p);
    double_sqrt_example(q*q);
    double_sqrt_example(r*r);
    double_sqrt_example(p*r);
 }
  for (k = 1; k < 7; k++) {
    double_sqrt_example((1.0) / (2 * j + 1));
    double_sqrt_example((1.0) / (2 * k + 35));
  }
  for (k = 2; k < 38; k++) {
    double_sqrt_example(dtiny * k);
  }
  double_sqrt_example(dtiny);
  double_sqrt_example(-dtiny);
  double_sqrt_example(dtiny * 2.0);
  double_sqrt_example(-dtiny * 2.0);
  double_sqrt_example(dhuge);
  double_sqrt_example(-dhuge);
  double_sqrt_example(dhuge / 2.0);
  double_sqrt_example(-dhuge / 2.0);
  double_sqrt_example(dsemihuge);
  double_sqrt_example(-dsemihuge);
  double_sqrt_example(dsemihuge / 2.0);
  double_sqrt_example(-dsemihuge / 2.0);
  printf("    };\n");
  printf("    return result;\n");
  printf("  };\n");
  printf("\n");
  printf("}\n");
  return 0;
}

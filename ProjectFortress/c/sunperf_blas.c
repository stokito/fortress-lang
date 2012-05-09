#include "com_sun_fortress_numerics_Blas.h"

#include <jni.h>

#if defined(OSX)
#include <Accelerate/Accelerate.h>

#elif defined(SUNOS)
#include <sunperf.h> 

#else
#include <cblas.h>

#endif

#ifdef __cplusplus
extern "C" {
#endif

static double oneVectorFunction( JNIEnv * env, jint length,
                               jdoubleArray vector1, jint stride1, jint offset1,
                               double (*cblas)() ){

    double ret = 0;
    int i;
    jboolean copy;
    // jdouble * convertedVector1 = (*env)->GetDoubleArrayElements(env, vector1, &copy);
    jdouble * convertedVector1 = (*env)->GetPrimitiveArrayCritical(env, vector1, &copy);

    /* for debugging, if you want
       printf( "length is %d\n", length );
       for ( i = 0; i < length; i++ ){
       printf( "vector1 [%d] %f\n", i, convertedVector1[i] );
       printf( "vector2 [%d] %f\n", i, convertedVector2[i] );
       }
       */

    ret = (*cblas)( length, convertedVector1 + offset1, stride1 );
    // (*env)->ReleaseDoubleArrayElements(env, vector1, convertedVector1, 0 );
    (*env)->ReleasePrimitiveArrayCritical(env, vector1, convertedVector1, 0 );
    return ret;

}

static double oneVectorAlphaVoidFunction( JNIEnv * env, jint length, jdouble alpha,
                                          jdoubleArray vector1, jint stride1, jint offset1,
                                          void (*cblas)() ){

    double ret = 0;
    int i;
    // jdouble * convertedVector1 = (*env)->GetDoubleArrayElements(env, vector1, 0);
    jdouble * convertedVector1 = (*env)->GetPrimitiveArrayCritical(env, vector1, 0);

    /* for debugging, if you want
       printf( "length is %d\n", length );
       for ( i = 0; i < length; i++ ){
       printf( "vector1 [%d] %f\n", i, convertedVector1[i] );
       printf( "vector2 [%d] %f\n", i, convertedVector2[i] );
       }
       */

    (*cblas)( length, alpha, convertedVector1 + offset1, stride1 );
    // (*env)->ReleaseDoubleArrayElements(env, vector1, convertedVector1, 0 );
    (*env)->ReleasePrimitiveArrayCritical(env, vector1, convertedVector1, 0 );
}

static void twoVectorVoidFunction( JNIEnv * env, jint length,
                               jdoubleArray vector1, jint stride1, jint offset1,
                               jdoubleArray vector2, jint stride2, jint offset2,
                               void (*cblas)() ){

    int i;
    // jdouble * convertedVector1 = (*env)->GetDoubleArrayElements(env, vector1, 0);
    jdouble * convertedVector1 = (*env)->GetPrimitiveArrayCritical(env, vector1, 0);
    // jdouble * convertedVector2 = (*env)->GetDoubleArrayElements(env, vector2, 0);
    jdouble * convertedVector2 = (*env)->GetPrimitiveArrayCritical(env, vector2, 0);

    /* for debugging, if you want
       printf( "length is %d\n", length );
       for ( i = 0; i < length; i++ ){
       printf( "vector1 [%d] %f\n", i, convertedVector1[i] );
       printf( "vector2 [%d] %f\n", i, convertedVector2[i] );
       }
       */

    (*cblas)( length, convertedVector1 + offset1, stride1, convertedVector2 + offset2, stride2 );
    // (*env)->ReleaseDoubleArrayElements(env, vector1, convertedVector1, 0 );
    (*env)->ReleasePrimitiveArrayCritical(env, vector1, convertedVector1, 0 );
    // (*env)->ReleaseDoubleArrayElements(env, vector2, convertedVector2, 0 );
    (*env)->ReleasePrimitiveArrayCritical(env, vector2, convertedVector2, 0 );
}

static double twoVectorFunction( JNIEnv * env, jint length,
                               jdoubleArray vector1, jint stride1, jint offset1,
                               jdoubleArray vector2, jint stride2, jint offset2,
                               double (*cblas)() ){

    double ret = 0;
    int i;
    // jdouble * convertedVector1 = (*env)->GetDoubleArrayElements(env, vector1, 0);
    jdouble * convertedVector1 = (*env)->GetPrimitiveArrayCritical(env, vector1, 0);
    // jdouble * convertedVector2 = (*env)->GetDoubleArrayElements(env, vector2, 0);
    jdouble * convertedVector2 = (*env)->GetPrimitiveArrayCritical(env, vector2, 0);

    /* for debugging, if you want
       printf( "length is %d\n", length );
       for ( i = 0; i < length; i++ ){
       printf( "vector1 [%d] %f\n", i, convertedVector1[i] );
       printf( "vector2 [%d] %f\n", i, convertedVector2[i] );
       }
       */

    ret = (*cblas)( length, convertedVector1 + offset1, stride1, convertedVector2 + offset2, stride2 );
    // (*env)->ReleaseDoubleArrayElements(env, vector1, convertedVector1, 0 );
    (*env)->ReleasePrimitiveArrayCritical(env, vector1, convertedVector1, 0 );
    // (*env)->ReleaseDoubleArrayElements(env, vector2, convertedVector2, 0 );
    (*env)->ReleasePrimitiveArrayCritical(env, vector2, convertedVector2, 0 );
    return ret;

}

static void twoVectorAlphaVoidFunction( JNIEnv * env, jint length, jdouble alpha,
                                    jdoubleArray vector1, jint stride1, jint offset1,
                                    jdoubleArray vector2, jint stride2, jint offset2,
                                    void (*cblas)() ){

    int i;
    // jdouble * convertedVector1 = (*env)->GetDoubleArrayElements(env, vector1, 0);
    jdouble * convertedVector1 = (*env)->GetPrimitiveArrayCritical(env, vector1, 0);
    // jdouble * convertedVector2 = (*env)->GetDoubleArrayElements(env, vector2, 0);
    jdouble * convertedVector2 = (*env)->GetPrimitiveArrayCritical(env, vector2, 0);

    /* for debugging, if you want
       printf( "length is %d\n", length );
       for ( i = 0; i < length; i++ ){
       printf( "vector1 [%d] %f\n", i, convertedVector1[i] );
       printf( "vector2 [%d] %f\n", i, convertedVector2[i] );
       }
       */

    (*cblas)( length, alpha, convertedVector1 + offset1, stride1, convertedVector2 + offset2, stride2 );
    // (*env)->ReleaseDoubleArrayElements(env, vector1, convertedVector1, 0 );
    // (*env)->ReleaseDoubleArrayElements(env, vector2, convertedVector2, 0 );
    
    (*env)->ReleasePrimitiveArrayCritical(env, vector1, convertedVector1, 0 );
    (*env)->ReleasePrimitiveArrayCritical(env, vector2, convertedVector2, 0 );
}

/*
 * Class:     com_sun_fortress_numerics_Blas
 * Method:    dotProduct
 * Signature: ([D[D)D
 */
JNIEXPORT jdouble JNICALL Java_com_sun_fortress_numerics_Blas_dotProduct
  (JNIEnv * env, jclass class_, jint length, jdoubleArray vector1, jint stride1, jint offset1, jdoubleArray vector2, jint stride2, jint offset2 ){

      return twoVectorFunction( env, length, vector1, stride1, offset1, vector2, stride2, offset2, ddot );

}

JNIEXPORT jdouble JNICALL Java_com_sun_fortress_numerics_Blas_norm
  (JNIEnv * env, jclass class_, jint length, jdoubleArray vector1, jint stride1, jint offset1 ){
      return oneVectorFunction( env, length, vector1, stride1, offset1, dnrm2 );
}

JNIEXPORT void JNICALL Java_com_sun_fortress_numerics_Blas_internal_1add
  (JNIEnv * env, jclass class_, jint length, jdouble alpha, jdoubleArray vector1, jint stride1, jint offset1, jdoubleArray vector2, jint stride2, jint offset2 ){
      twoVectorAlphaVoidFunction( env, length, alpha, vector1, stride1, offset1, vector2, stride2, offset2, daxpy );
}

JNIEXPORT void JNICALL Java_com_sun_fortress_numerics_Blas_internal_1dcopy
  (JNIEnv * env, jclass class_, jint length, jdoubleArray vector1, jint stride1, jint offset1, jdoubleArray vector2, jint stride2, jint offset2){
      twoVectorVoidFunction( env, length, vector1, stride1, offset1, vector2, stride2, offset2, dcopy );
}

JNIEXPORT void JNICALL Java_com_sun_fortress_numerics_Blas_internal_1scale
  (JNIEnv * env, jclass class_, jint length, jdouble alpha, jdoubleArray vector, jint stride, jint offset){
      oneVectorAlphaVoidFunction( env, length, alpha, vector, stride, offset, dscal );
}


JNIEXPORT void JNICALL Java_com_sun_fortress_numerics_Blas_internal_1vectorMatrixMultiply
  (JNIEnv * env, jclass class_, jint order, jint transpose, jint m, jint n, jdouble alpha, jdoubleArray matrix, jint matrixStride, jint matrixOffset, jdoubleArray vector, jint stride, jint offset, jdouble beta, jdoubleArray result, jint resultStride, jint resultOffset){


      // jdouble * convertedMatrix = (*env)->GetDoubleArrayElements(env, matrix, 0);
      // jdouble * convertedVector = (*env)->GetDoubleArrayElements(env, vector, 0);
      // jdouble * convertedResult = (*env)->GetDoubleArrayElements(env, result, 0);
      
      jdouble * convertedMatrix = (*env)->GetPrimitiveArrayCritical(env, matrix, 0);
      jdouble * convertedVector = (*env)->GetPrimitiveArrayCritical(env, vector, 0);
      jdouble * convertedResult = (*env)->GetPrimitiveArrayCritical(env, result, 0);

      dgemv( transpose, m, n, alpha,
                   convertedMatrix + matrixOffset, matrixStride,
                   convertedVector + offset, stride, beta,
                   convertedResult + resultOffset, resultStride );

      // (*env)->ReleaseDoubleArrayElements(env, matrix, convertedMatrix, 0 );
      // (*env)->ReleaseDoubleArrayElements(env, vector, convertedVector, 0 );
      // (*env)->ReleaseDoubleArrayElements(env, result, convertedResult, 0 );
      
      (*env)->ReleasePrimitiveArrayCritical(env, matrix, convertedMatrix, 0 );
      (*env)->ReleasePrimitiveArrayCritical(env, vector, convertedVector, 0 );
      (*env)->ReleasePrimitiveArrayCritical(env, result, convertedResult, 0 );
}

JNIEXPORT void JNICALL Java_com_sun_fortress_numerics_Blas_internal_1matrixMatrixMultiply
  (JNIEnv * env, jclass class_, jint order, jint transpose1, jint transpose2, jint m, jint n, jint k, jdouble alpha, jdoubleArray matrix1, jint matrix1Stride, jint matrix1Offset, jdoubleArray matrix2, jint matrix2Stride, jint matrix2Offset, jdouble beta, jdoubleArray resultMatrix, jint resultStride, jint resultOffset){
      
      jdouble * convertedMatrix1 = (*env)->GetPrimitiveArrayCritical(env, matrix1, 0);
      jdouble * convertedMatrix2 = (*env)->GetPrimitiveArrayCritical(env, matrix2, 0);
      jdouble * convertedResult = (*env)->GetPrimitiveArrayCritical(env, resultMatrix, 0);

      dgemm( transpose1, transpose2, m, n, k, alpha, convertedMatrix1 + matrix1Offset, matrix1Stride, convertedMatrix2 + matrix2Offset, matrix2Stride, beta, convertedResult + resultOffset, resultStride );
      
      (*env)->ReleasePrimitiveArrayCritical(env, matrix1, convertedMatrix1, 0 );
      (*env)->ReleasePrimitiveArrayCritical(env, matrix2, convertedMatrix2, 0 );
      (*env)->ReleasePrimitiveArrayCritical(env, resultMatrix, convertedResult, 0 );
}
    
#ifdef __cplusplus
}
#endif

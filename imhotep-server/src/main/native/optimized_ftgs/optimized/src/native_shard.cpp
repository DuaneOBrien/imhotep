#include <jni.h>

#undef  JNIEXPORT
#define JNIEXPORT __attribute__((visibility("default")))

#include "jni/com_indeed_imhotep_local_NativeShard.h"

#include "jni_util.hpp"
#include "shard.hpp"

using namespace imhotep;
using namespace std;

/*
 * Class:     com_indeed_imhotep_local_NativeShard
 * Method:    nativeGetShard
 * Signature: (Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;J[Ljava/lang/String;[J)J
 */
JNIEXPORT jlong JNICALL
Java_com_indeed_imhotep_local_NativeShard_nativeGetShard(JNIEnv*      env,
                                                         jclass       unusedClass,
                                                         jstring      shardDir,
                                                         jobjectArray intFields,
                                                         jobjectArray strFields,
                                                         jlong        packedTablePtr,
                                                         jobjectArray mappedFiles,
                                                         jlongArray   mappedPtrs)
{
    const string shard_dir(from_java<jstring, string>(env, shardDir));

    const strvec_t int_fields(from_java_array<string>(env, intFields));
    const strvec_t str_fields(from_java_array<string>(env, strFields));

    const Shard::packed_table_ptr packed_table_ptr(from_java<jlong,
                                                   Shard::packed_table_ptr>(env, packedTablePtr));

    const strvec_t     mapped_files(from_java_array<string>(env, mappedFiles));
    const vector<long> mapped_ptrs(from_java_array<long>(env, mappedPtrs));
    die_if(mapped_files.size() != mapped_ptrs.size(),
           "mapped_files.size() != mapped_ptrs.size()");

    Shard::MapCache map_cache;
    for (size_t idx(0); idx < mapped_files.size(); ++idx) {
        map_cache[mapped_files[idx]] = reinterpret_cast<void*>(mapped_ptrs[idx]);
    }

    try {
        Shard* result(new Shard(shard_dir, int_fields, str_fields,
                                packed_table_ptr, map_cache));
        return reinterpret_cast<jlong>(result);
    }
    catch (const std::exception& ex) {
        jclass exClass = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exClass, ex.what());
    }
    return 0L;
}

/*
 * Class:     com_indeed_imhotep_local_NativeShard
 * Method:    nativeReleaseShard
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_indeed_imhotep_local_NativeShard_nativeReleaseShard(JNIEnv* env,
                                                             jclass  unusedClass,
                                                             jlong   nativeShardPtr)
{
    Shard* shardPtr(reinterpret_cast<Shard*>(nativeShardPtr));
    if (shardPtr != 0) {
        delete shardPtr;
    }
}

/*
 * Class:     com_indeed_imhotep_local_NativeShard
 * Method:    nativeToString
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_indeed_imhotep_local_NativeShard_nativeToString(JNIEnv* env,
                                                         jclass  unusedClass,
                                                         jlong   nativeShardPtr)
{
    const Shard* shardPtr(reinterpret_cast<Shard*>(nativeShardPtr));
    const string result(shardPtr != 0 ? shardPtr->to_string() : "{ null }");
    return env->NewStringUTF(result.c_str());
}


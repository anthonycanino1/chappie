#include <stdio.h>
#include <jni.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <math.h>
#include <stdint.h>
#include <string.h>
#include<inttypes.h>
#include "CPUScaler.h"
#include "arch_spec.h"
#include "msr.h"

rapl_msr_unit rapl_unit;
rapl_msr_parameter *parameters;
char *ener_info;
/*global variable*/
int *fd;

void copy_to_string(char *ener_info, char uncore_buffer[60], int uncore_num, char cpu_buffer[60], int cpu_num, char package_buffer[60], int package_num, int i, int *offset) {
	memcpy(ener_info + *offset, &uncore_buffer, uncore_num);
	//split sigh
	ener_info[*offset + uncore_num] = '#';
	memcpy(ener_info + *offset + uncore_num + 1, &cpu_buffer, cpu_num);
	ener_info[*offset + uncore_num + cpu_num + 1] = '#';
	if(i < num_pkg - 1) {
		memcpy(ener_info + *offset + uncore_num + cpu_num + 2, &package_buffer, package_num);
		offset += uncore_num + cpu_num + package_num + 2;
		if(num_pkg > 1) {
			ener_info[*offset] = '@';
			offset++;
		}
	} else {
		memcpy(ener_info + *offset + uncore_num + cpu_num + 2, &package_buffer, package_num + 1);
	}

}


JNIEXPORT jint JNICALL Java_jrapl_EnergyCheckUtils_ProfileInit(JNIEnv *env, jclass jcls) {
	int i;
	char msr_filename[BUFSIZ];

	get_cpu_model();	
	getSocketNum();

	jint wraparound_energy;

	/*only two domains are supported for parameters check*/
	parameters = (rapl_msr_parameter *)malloc(2 * sizeof(rapl_msr_parameter));
	fd = (int *) malloc(num_pkg * sizeof(int));

	for(i = 0; i < num_pkg; i++) {
		if(i > 0) {
			core += num_pkg_thread / 2; 	//measure the first core of each package
		}
		sprintf(msr_filename, "/dev/cpu/%d/msr", core);
		fd[i] = open(msr_filename, O_RDWR);
	}

	uint64_t unit_info = read_msr(fd[0], MSR_RAPL_POWER_UNIT);
	//printf("open core: %d\n", core);
	get_msr_unit(&rapl_unit, unit_info);
	get_wraparound_energy(rapl_unit.energy);
	wraparound_energy = (int)WRAPAROUND_VALUE;

	return wraparound_energy;
}

JNIEXPORT jint JNICALL Java_jrapl_EnergyCheckUtils_GetSocketNum(JNIEnv *env, jclass jcls) {
	return (jint)getSocketNum();
}

// If you want to keep it 'optimized' you can use a global buffer and only
// allocate if you actually can't fit.
char ener_info_buf[512];

JNIEXPORT jstring JNICALL Java_jrapl_EnergyCheckUtils_EnergyStatCheck(JNIEnv *env,
		jclass jcls) {
	jstring ener_string;
	double result = 0.0;
	double package[num_pkg];
	double pp0[num_pkg];
	double pp1[num_pkg];
	double dram[num_pkg];
	//construct a string
	char *local_ener_info = NULL;
	long dram_num = 0L; 
	long cpu_num = 0L; 
	long gpu_num = 0L; 
	long package_num = 0L;
	int info_size;
	int i;
	int offset = 0;


	for(i = 0; i < num_pkg; i++) {
		char gpu_buffer[60]; 
		char dram_buffer[60]; 
		char cpu_buffer[60]; 
		char package_buffer[60];

		result = read_msr(fd[i], MSR_PKG_ENERGY_STATUS);	//First 32 bits so don't need shift bits.
		package[i] = (double) result * rapl_unit.energy;

		result = read_msr(fd[i], MSR_PP0_ENERGY_STATUS);
		pp0[i] = (double) result * rapl_unit.energy;

		//printf("package energy: %f\n", package[i]);

		sprintf(package_buffer, "%f", package[i]);
		sprintf(cpu_buffer, "%f", pp0[i]);
		
		//allocate space for string
		//printf("%" PRIu32 "\n", cpu_model);
		//printf("Hex:%x" PRIu32 "\n", cpu_model);

		switch(cpu_model) {
			case SANDYBRIDGE_EP:
			case HASWELL1:
			case HASWELL2:
			case HASWELL3:
			case HASWELL_EP:
			case SKYLAKE1:
			case SKYLAKE2:
	
				result = read_msr(fd[i],MSR_DRAM_ENERGY_STATUS);
				dram[i] =(double)result*rapl_unit.energy;

				sprintf(dram_buffer, "%f", dram[i]);

				package_num = strlen(package_buffer);
				dram_num = strlen(dram_buffer);
				cpu_num = strlen(cpu_buffer);

				if(i == 0) {
					info_size = (num_pkg * (dram_num + cpu_num + package_num + 4));	
					local_ener_info = (char *) ener_info_buf;
					/*
					printf("info_size:%d\n", info_size);

					int j;
					for (j = 0; j < 8; j++) {
						local_ener_info[info_size+j] = 0x0A;
					}
					*/
				}

				
				//copy_to_string(local_ener_info, dram_buffer, dram_num, cpu_buffer, cpu_num, package_buffer, package_num, i, &offset);
				/*Insert socket number*/	
				
				memcpy(local_ener_info + offset, &dram_buffer, dram_num);
				//printf("Wrote %d at offset %d\n", dram_num, offset);

				//split sigh
				local_ener_info[offset + dram_num] = '#';
				//printf("Wrote %d at offset %d\n", 1, offset + dram_num);

				memcpy(local_ener_info + offset + dram_num + 1, &cpu_buffer, cpu_num);
				//printf("Wrote %d at offset %d\n", cpu_num, offset + dram_num + 1);

				local_ener_info[offset + dram_num + cpu_num + 1] = '#';
				//printf("Wrote %d at offset %d\n", 1, offset + dram_num + cpu_num + 1);

				if(i < num_pkg - 1) {
					memcpy(local_ener_info + offset + dram_num + cpu_num + 2, &package_buffer, package_num);
					//printf("Wrote %d at offset %d\n", package_num, offset + dram_num + cpu_num + 2);

					offset += dram_num + cpu_num + package_num + 2;
					if(num_pkg > 1) {
						local_ener_info[offset] = '@';
						//printf("Wrote %d at offset %d\n", 1, offset);
						offset++;
					}
				} else {
					memcpy(local_ener_info + offset + dram_num + cpu_num + 2, &package_buffer, package_num + 1);
					//printf("Wrote %d at offset %d\n", package_num + 1, offset + dram_num + cpu_num + 2);
				}

				break;	
			case SANDYBRIDGE:
			case IVYBRIDGE:


				result = read_msr(fd[i],MSR_PP1_ENERGY_STATUS);
				pp1[i] = (double) result *rapl_unit.energy;

				sprintf(gpu_buffer, "%f", pp1[i]);

				package_num = strlen(package_buffer);
				gpu_num = strlen(gpu_buffer);		
				cpu_num = strlen(cpu_buffer);
				if(i == 0) {
					info_size = num_pkg * (gpu_num + cpu_num + package_num + 4);	
					local_ener_info = (char *) malloc(info_size);
				}

				//copy_to_string(local_ener_info, gpu_buffer, gpu_num, cpu_buffer, cpu_num, package_buffer, package_num, i, &offset);
				
				memcpy(local_ener_info + offset, &gpu_buffer, gpu_num);
				//split sign
				local_ener_info[offset + gpu_num] = '#';
				memcpy(local_ener_info + offset + gpu_num + 1, &cpu_buffer, cpu_num);
				local_ener_info[offset + gpu_num + cpu_num + 1] = '#';
				if(i < num_pkg - 1) {
					memcpy(local_ener_info + offset + gpu_num + cpu_num + 2, &package_buffer, package_num);
					offset += gpu_num + cpu_num + package_num + 2;
					if(num_pkg > 1) {
						local_ener_info[offset] = '@';
						offset++;
					}
				} else {
					memcpy(local_ener_info + offset + gpu_num + cpu_num + 2, &package_buffer,
							package_num + 1);
				}
				
				break;
		default:
				printf("non of archtectures are detected\n");
				break;


		}
	} 

	/*
	printf("-- Byte Dump --\n");
	int j;
	printf("0x");
	for (j = 0; j < 8; j++) {
		printf("%X", local_ener_info[info_size+j], sizeof local_ener_info[info_size+j]);
	}
	printf("\n");

	for (j = 0; j < 8; j++) {
		if (local_ener_info[info_size+j] != 0xA) {
			printf("Detected overflow, exiting!\n");
			exit(1);
		}
	} 
	*/

	ener_string = (*env)->NewStringUTF(env, local_ener_info);	

	return ener_string;
}

JNIEXPORT void JNICALL Java_jrapl_EnergyCheckUtils_ProfileDealloc
   (JNIEnv * env, jclass jcls) {
	free(fd);	
	free(parameters);
}


#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <omp.h>

using namespace std;

#define SYSTEMTIME clock_t
#define SYSTEMTIME2 double

void OnMult(int m_ar, int m_br)
{
	SYSTEMTIME Time1, Time2;

	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	Time1 = clock();

	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
		{
			temp = 0;
			for (k = 0; k < m_ar; k++)
			{
				temp += pha[i * m_ar + k] * phb[k * m_br + j];
			}
			phc[i * m_ar + j] = temp;
		}
	}

	Time2 = clock();
	sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
	cout << st;

	// display 10 elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

// add code here for line x line matriz multiplication
void OnMultLine(int m_ar, int m_br)
{
	SYSTEMTIME Time1, Time2;

	char st[100];
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)calloc((m_ar * m_ar), sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	/* exemplo:

	double matrixA[9] = {1, 2, 3, 4, 5, 6, 7, 8, 9};
	double matrixB[9] = {9, 8, 7, 6, 5, 4, 3, 2, 1};
	//double resultado[9] = {30, 24, 18, 84, 69, 54, 138, 114, 90};

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_br; j++)
		{
			pha[i * m_ar + j] = matrixA[i * m_ar + j];
			phb[i * m_br + j] = matrixB[i * m_br + j];
		}
	*/

	Time1 = clock();

	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_ar; k++)
		{
			for (j = 0; j < m_br; j++)
			{
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

	Time2 = clock();
	double timeInSeconds = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
	double gflops = (2.0 * m_ar * m_ar * m_br) / (timeInSeconds * 1e9);

	sprintf(st, "Time: %3.3f seconds\n", timeInSeconds);
	cout << st;
	cout << "Performance: " << gflops << " GFLOPS" << endl;

	// display 10 elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	/*
	// display all elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
			cout << phc[i * m_ar + j] << " ";
		cout << endl;
	}
	*/

	free(pha);
	free(phb);
	free(phc);
}

// add code here for block x block matriz multiplication
void OnMultBlock(int m_ar, int m_br, int bkSize)
{
	SYSTEMTIME Time1, Time2;

	char st[100];
	int i, j, k, ii, jj, kk;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)calloc((m_ar * m_ar), sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	/* exemplo: 

	double matrixA[16] = {1, 2, 2, 7, 1, 5, 6, 2, 3, 3, 4, 5, 3, 3, 6, 7};
	double matrixB[16] = {3, 1, 1, 4, 7, 4, 5, 3, 2, 4, 2, 4, 3, 1, 8, 5};
	//double resultado[16] = {42, 24, 71, 53, 56, 47, 54, 53, 53, 36, 66, 62, 63, 46, 86, 80};

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_br; j++)
		{
			pha[i * m_ar + j] = matrixA[i * m_ar + j];
			phb[i * m_br + j] = matrixB[i * m_br + j];
		}
	*/

	Time1 = clock();

	for (ii = 0; ii < m_ar; ii += bkSize)
		for (kk = 0; kk < m_ar; kk += bkSize)
			for (jj = 0; jj < m_br; jj += bkSize)
				for (i = ii; i < min(ii + bkSize, m_ar); i++)
					for (k = kk; k < min(kk + bkSize, m_ar); k++)
						for (j = jj; j < min(jj + bkSize, m_br); j++)
							phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];

	Time2 = clock();
	sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	/*
	//display all elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
			cout << phc[i * m_ar + j] << " ";
		cout << endl;
	}
	*/

	free(pha);
	free(phb);
	free(phc);
}

void OnMultLineOMP1(int m_ar, int m_br)
{
	SYSTEMTIME2 Time1, Time2;

	char st[100];
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)calloc((m_ar * m_ar), sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	/* exemplo:

	double matrixA[9] = {1, 2, 3, 4, 5, 6, 7, 8, 9};
	double matrixB[9] = {9, 8, 7, 6, 5, 4, 3, 2, 1};
	//double resultado[9] = {30, 24, 18, 84, 69, 54, 138, 114, 90};

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_br; j++)
		{
			pha[i * m_ar + j] = matrixA[i * m_ar + j];
			phb[i * m_br + j] = matrixB[i * m_br + j];
		}
	*/

	Time1 = omp_get_wtime();

	#pragma omp parallel for private(j, k)
	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_ar; k++)
		{
			for (j = 0; j < m_br; j++)
			{
				phc[i * m_br + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

	Time2 = omp_get_wtime();

	sprintf(st, "Time: %3.3f seconds\n", Time2 - Time1);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	/*
	// display all elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
			cout << phc[i * m_ar + j] << " ";
		cout << endl;
	}
	*/

	free(pha);
	free(phb);
	free(phc);
}

void OnMultLineOMP2(int m_ar, int m_br)
{
	SYSTEMTIME2 Time1, Time2;

	char st[100];
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)calloc((m_ar * m_ar), sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	/* exemplo:
	
	double matrixA[9] = {1, 2, 3, 4, 5, 6, 7, 8, 9};
	double matrixB[9] = {9, 8, 7, 6, 5, 4, 3, 2, 1};
	//double resultado[9] = {30, 24, 18, 84, 69, 54, 138, 114, 90};

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_br; j++)
		{
			pha[i * m_ar + j] = matrixA[i * m_ar + j];
			phb[i * m_br + j] = matrixB[i * m_br + j];
		}
	*/

	Time1 = omp_get_wtime();

	#pragma omp parallel private(i, k)
	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_ar; k++)
		{
			#pragma omp for
			for (j = 0; j < m_br; j++)
			{
				phc[i * m_br + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

	Time2 = omp_get_wtime();

	sprintf(st, "Time: %3.3f seconds\n", Time2 - Time1);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	/*
	// display all elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
			cout << phc[i * m_ar + j] << " ";
		cout << endl;
	}
	*/

	free(pha);
	free(phb);
	free(phc);
}

void handle_error(int retval)
{
	printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
	exit(1);
}

void init_papi()
{
	int retval = PAPI_library_init(PAPI_VER_CURRENT);
	if (retval != PAPI_VER_CURRENT && retval < 0)
	{
		printf("PAPI library version mismatch!\n");
		exit(1);
	}
	if (retval < 0)
		handle_error(retval);

	std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
			  << " MINOR: " << PAPI_VERSION_MINOR(retval)
			  << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

int main(int argc, char *argv[])
{

	char c;
	int lin, col, blockSize;
	int op;

	int EventSet = PAPI_NULL;
	long long values[2];
	int ret;

	ret = PAPI_library_init(PAPI_VER_CURRENT);
	if (ret != PAPI_VER_CURRENT)
		std::cout << "FAIL" << endl;

	ret = PAPI_create_eventset(&EventSet);
	if (ret != PAPI_OK)
		cout << "ERROR: create eventset" << endl;

	ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
	if (ret != PAPI_OK)
		cout << "ERROR: PAPI_L1_DCM" << endl;

	ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
	if (ret != PAPI_OK)
		cout << "ERROR: PAPI_L2_DCM" << endl;

	op = 1;
	do
	{
		cout << endl
			 << "1. Multiplication" << endl;
		cout << "2. Line Multiplication" << endl;
		cout << "3. Block Multiplication" << endl;
		cout << "4. Line Multiplication OMP V1" << endl;
		cout << "5. Line Multiplication OMP V2" << endl;
		cout << "Selection?: ";
		cin >> op;
		if (op == 0)
			break;
		printf("Dimensions: lins=cols ? ");
		cin >> lin;
		col = lin;

		// Start counting
		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK)
			cout << "ERROR: Start PAPI" << endl;

		switch (op)
		{
		case 1:
			OnMult(lin, col);
			break;
		case 2:
			OnMultLine(lin, col);
			break;
		case 3:
			cout << "Block Size? ";
			cin >> blockSize;
			OnMultBlock(lin, col, blockSize);
			break;
		case 4:
			OnMultLineOMP1(lin, col);
			break;
		case 5:
			OnMultLineOMP2(lin, col);
			break;
		}

		ret = PAPI_stop(EventSet, values);
		if (ret != PAPI_OK)
			cout << "ERROR: Stop PAPI" << endl;
		printf("L1 DCM: %lld \n", values[0]);
		printf("L2 DCM: %lld \n", values[1]);

		ret = PAPI_reset(EventSet);
		if (ret != PAPI_OK)
			std::cout << "FAIL reset" << endl;

	} while (op != 0);

	ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
	if (ret != PAPI_OK)
		std::cout << "FAIL remove event" << endl;

	ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
	if (ret != PAPI_OK)
		std::cout << "FAIL remove event" << endl;

	ret = PAPI_destroy_eventset(&EventSet);
	if (ret != PAPI_OK)
		std::cout << "FAIL destroy" << endl;
}
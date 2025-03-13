#include <stdio.h>
#include <omp.h>

int main() {
    int num_threads = omp_get_max_threads();
    printf("Número máximo de threads disponíveis: %d\n", num_threads);
    return 0;
}
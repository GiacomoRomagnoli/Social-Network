export interface Probe {
    isReady(): Promise<void>
}